<#
.SYNOPSIS
    COU_Server를 AWS EC2(cou-server)에 배포한다. (Spring 심화 2-6 수동 배포를 자동화)

.DESCRIPTION
    1. (선택) git pull
    2. gradlew bootJar 로 jar 빌드
    3. scp 로 EC2 ~/cou/app-new.jar 업로드
    4. EC2에서 MySQL/Redis(docker compose) 확인 -> 기존 서버 종료 -> app.jar 를 app-old.jar 로 백업 -> 교체 -> nohup 실행
    5. app.log 에 "Started CouServerApplication" 이 뜰 때까지 대기
    6. 내 PC에서 http://{EC2}:8080 응답 확인

.EXAMPLE
    .\deploy.ps1               # 현재 코드 그대로 빌드 후 배포
    .\deploy.ps1 -Pull         # git pull 후 빌드, 배포
    .\deploy.ps1 -SkipBuild    # 빌드 생략, build\libs 에 있는 jar 를 그대로 배포
    .\deploy.ps1 -Rollback     # 직전 버전(app-old.jar)으로 되돌리기

.NOTES
    스크립트 실행이 막혀 있으면:
    powershell -ExecutionPolicy Bypass -File .\deploy.ps1
#>
param(
    [string]$Ec2Host = "3.34.14.51",
    [string]$User    = "ec2-user",
    [string]$KeyPath = "$env:USERPROFILE\.ssh\cou-key.pem",
    [switch]$Pull,
    [switch]$SkipBuild,
    [switch]$Rollback
)

$ErrorActionPreference = "Stop"
Set-Location -Path $PSScriptRoot

$Target  = "$User@$Ec2Host"
$SshOpts = @("-i", $KeyPath, "-o", "ConnectTimeout=10", "-o", "StrictHostKeyChecking=accept-new")

function Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Fail($msg) { Write-Host "`n[실패] $msg" -ForegroundColor Red; exit 1 }

# EC2에서 실행할 bash 스크립트 (주석과 메시지는 영문 ASCII 로만 유지: PowerShell 5.1 파이프 인코딩 문제 방지)
function Get-RemoteScript([string]$mode) {
    $script = @'
set -u
MODE="__MODE__"
cd ~/cou || { echo "[remote] ~/cou not found"; exit 1; }

if [ "$MODE" = "deploy" ] && [ ! -f app-new.jar ]; then
  echo "[remote] app-new.jar not found"; exit 1
fi
if [ "$MODE" = "rollback" ] && [ ! -f app-old.jar ]; then
  echo "[remote] app-old.jar not found (no previous version to roll back to)"; exit 1
fi

echo "[remote] ensure mysql/redis are running"
docker compose up -d >/dev/null 2>&1 || echo "[remote] WARN: docker compose up failed"

PATTERN='java .*-jar app\.jar'
if pgrep -f "$PATTERN" >/dev/null; then
  echo "[remote] stopping current server (pid $(pgrep -f "$PATTERN" | tr '\n' ' '))"
  pkill -f "$PATTERN"
  for i in $(seq 1 20); do pgrep -f "$PATTERN" >/dev/null || break; sleep 1; done
  if pgrep -f "$PATTERN" >/dev/null; then
    echo "[remote] force kill"; pkill -9 -f "$PATTERN"; sleep 1
  fi
fi

if [ "$MODE" = "deploy" ]; then
  [ -f app.jar ] && mv -f app.jar app-old.jar
  mv -f app-new.jar app.jar
  echo "[remote] app.jar replaced (previous version kept as app-old.jar)"
else
  [ -f app.jar ] && mv -f app.jar app-broken.jar
  mv -f app-old.jar app.jar
  echo "[remote] rolled back to app-old.jar (failed version kept as app-broken.jar)"
fi

[ -f app.log ] && mv -f app.log app.prev.log
setsid nohup java -Xmx512m -jar app.jar > app.log 2>&1 < /dev/null &
echo "[remote] started, waiting for Spring boot..."

for i in $(seq 1 120); do
  if grep -q "Started CouServerApplication" app.log 2>/dev/null; then
    grep "Started CouServerApplication" app.log | tail -n 1
    echo "[remote] OK pid $(pgrep -f "$PATTERN" | tr '\n' ' ')"
    exit 0
  fi
  if ! pgrep -f "$PATTERN" >/dev/null; then
    echo "[remote] server process exited. last log lines:"
    tail -n 60 app.log
    exit 2
  fi
  sleep 1
done

echo "[remote] timeout (120s). last log lines:"
tail -n 60 app.log
exit 3
'@
    # CR 제거 (Windows 줄바꿈이 bash 에 들어가지 않도록)
    return ($script.Replace("__MODE__", $mode) -replace "`r", "")
}

function Invoke-Remote([string]$mode) {
    $remote = Get-RemoteScript $mode
    # Out-Host: 원격 출력을 화면에만 보여주고 함수 반환값(exit code)에 섞이지 않게 한다
    $remote | & ssh @SshOpts $Target "bash -s" | Out-Host
    return $LASTEXITCODE
}

function Test-Http {
    Step "외부 접속 확인: http://${Ec2Host}:8080"
    try {
        Invoke-WebRequest -Uri "http://${Ec2Host}:8080/" -UseBasicParsing -TimeoutSec 10 | Out-Null
        Write-Host "응답 OK" -ForegroundColor Green
    } catch {
        # 401/403/404 처럼 HTTP 응답이 왔다면 서버는 살아 있는 것
        if ($_.Exception.Response) {
            $code = [int]$_.Exception.Response.StatusCode
            Write-Host "응답 OK (HTTP $code, 루트 경로라 정상)" -ForegroundColor Green
        } else {
            Write-Host "응답 없음: $($_.Exception.Message)" -ForegroundColor Yellow
            Write-Host "보안 그룹 8080 규칙이나 EC2 상태를 확인하세요." -ForegroundColor Yellow
        }
    }
}

# ---------------------------------------------------------------- 사전 확인
if (-not (Test-Path $KeyPath)) { Fail "키 파일이 없습니다: $KeyPath" }
if (-not (Get-Command ssh -ErrorAction SilentlyContinue)) { Fail "ssh 명령을 찾을 수 없습니다 (Windows OpenSSH 클라이언트 필요)" }

# ---------------------------------------------------------------- 롤백
if ($Rollback) {
    Step "직전 버전으로 롤백 ($Target)"
    $rc = Invoke-Remote "rollback"
    if ($rc -ne 0) { Fail "롤백 실패 (exit $rc). 위 로그를 확인하세요." }
    Test-Http
    Write-Host "`n롤백 완료" -ForegroundColor Green
    exit 0
}

# ---------------------------------------------------------------- 1. git pull
if ($Pull) {
    Step "git pull"
    git pull
    if ($LASTEXITCODE -ne 0) { Fail "git pull 실패. 충돌이나 로컬 변경사항을 확인하세요." }
}
try {
    $branch = (git rev-parse --abbrev-ref HEAD)
    $commit = (git log -1 --format="%h %s")
    Write-Host "배포 대상: [$branch] $commit"
} catch { }

# ---------------------------------------------------------------- 2. 빌드
if (-not $SkipBuild) {
    Step "jar 빌드 (gradlew clean bootJar)"
    & .\gradlew.bat clean bootJar
    if ($LASTEXITCODE -ne 0) { Fail "빌드 실패" }
}

$jar = Get-ChildItem -Path "build\libs\*.jar" -ErrorAction SilentlyContinue |
       Where-Object { $_.Name -notlike "*-plain.jar" } |
       Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jar) { Fail "build\libs 에서 실행용 jar 를 찾지 못했습니다." }
Write-Host ("jar: {0} ({1:N1} MB, {2})" -f $jar.Name, ($jar.Length / 1MB), $jar.LastWriteTime)

# ---------------------------------------------------------------- 3. 업로드
Step "EC2 업로드 -> ~/cou/app-new.jar"
& scp @SshOpts $jar.FullName "${Target}:~/cou/app-new.jar"
if ($LASTEXITCODE -ne 0) { Fail "업로드 실패. EC2 상태, 보안 그룹 22번(내 IP) 규칙을 확인하세요." }

# ---------------------------------------------------------------- 4~5. 교체 및 재시작
Step "서버 교체 및 재시작"
$rc = Invoke-Remote "deploy"
if ($rc -ne 0) {
    Write-Host "`n새 버전이 정상 기동하지 않았습니다." -ForegroundColor Red
    Write-Host "직전 버전으로 되돌리려면:  .\deploy.ps1 -Rollback" -ForegroundColor Yellow
    exit $rc
}

# ---------------------------------------------------------------- 6. 외부 확인
Test-Http
Write-Host "`n배포 완료: http://${Ec2Host}:8080" -ForegroundColor Green
