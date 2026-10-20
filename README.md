# COU_Server

Force Push Syndrome 게임의 백엔드 서버입니다. 계정과 플레이어 데이터를 관리하고, 전투 결과·보상·상점·장비·친구·채팅 등의 API를 제공합니다. 게임의 정적 데이터는 서버에 포함된 JSON에서 읽어 클라이언트에 전달합니다.

## 기술 구성

- Java 21, Spring Boot 4.1.1, Gradle 9.7.1
- MySQL 8.4, Spring Data JPA
- Redis 7.4, Spring Security, JWT
- JUnit, Testcontainers

## 로컬 실행

Java 21과 Docker Compose가 필요합니다. 아래 명령은 저장소 루트에서 PowerShell로 실행합니다.

1. 환경 설정 파일을 만들고 `.env`의 `DB_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET`을 변경합니다.

   ```powershell
   Copy-Item .env.example .env
   ```

   `JWT_SECRET`에는 **32바이트 이상인 키를 Base64로 인코딩한 값**을 넣습니다. PowerShell에서 32바이트 키를 만들려면 다음 명령을 실행하고 출력값을 사용합니다.

   ```powershell
   $key = New-Object byte[] 32
   [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($key)
   [Convert]::ToBase64String($key)
   ```

2. MySQL과 Redis를 시작합니다. Compose는 `.env`의 비밀번호와 포트 설정을 사용합니다.

   ```powershell
   docker compose up -d
   ```

3. 서버를 실행합니다.

   ```powershell
   .\gradlew.bat bootRun
   ```

서버 기본 주소는 `http://localhost:8080`입니다. MySQL과 Redis는 기본적으로 각각 `127.0.0.1:3306`, `127.0.0.1:6379`에 열립니다. macOS/Linux에서는 `.env` 복사에 `cp`, Gradle 실행에 `./gradlew`를 사용하면 됩니다.

### 주요 환경 변수

| 변수 | 용도 | 기본값 |
|---|---|---|
| `DB_PASSWORD` | MySQL root 비밀번호 | 필수 |
| `DB_PORT` | 로컬 MySQL 포트 | `3306` |
| `REDIS_PASSWORD` | Redis 비밀번호 | 필수 |
| `REDIS_PORT` | 로컬 Redis 포트 | `6379` |
| `JWT_SECRET` | JWT 서명용 Base64 키 | 필수 |
| `DEV_TOOLS_ENABLED` | 개발용 API 및 편의 기능 활성화 | `false` |
| `AUTH_*`, `BATTLE_MAX_SPEED_MULTIPLIER` | 인증 시도 제한 및 전투 시간 검증 설정 | `.env.example` 참고 |

설정값의 전체 목록은 [.env.example](.env.example)과 [application.properties](src/main/resources/application.properties)를 참고하세요. `DEV_TOOLS_ENABLED`는 운영 환경에서 `false`로 유지해야 합니다.

## API 개요

| 기능 | 경로 |
|---|---|
| 회원가입·로그인 | `POST /api/accounts/signup`, `POST /api/accounts/login` |
| 정적 데이터 | `GET /api/static-data` |
| 플레이어 저장 데이터·능력치·진화·업적 | `/api/players/...` |
| 전투·챕터 보상 | `/api/battles/...`, `/api/chapter-chests/...` |
| 인벤토리·상점 | `/api/inventory/...`, `/api/shop/...` |
| 친구·채팅·접속 상태 | `/api/friends/...`, `/api/chat/...`, `/api/presence/...` |
| 랭킹 | `/api/ranking` |
| 실시간 채팅 | `ws://localhost:8080/ws/chat` |

회원가입, 로그인, 정적 데이터 조회를 제외한 HTTP API는 `Authorization: Bearer <accessToken>` 헤더가 필요합니다. 로그인 응답의 `accessToken`을 사용하세요. 채팅 WebSocket 연결에도 같은 인증 헤더가 필요합니다. 정적 데이터 API는 클라이언트의 `version` 쿼리값이 서버 버전과 같으면 `204 No Content`, 다르면 전체 데이터를 반환합니다.

## 테스트

통합 테스트는 Testcontainers로 MySQL과 Redis를 실행하므로 Docker가 실행 중이어야 합니다.

```powershell
.\gradlew.bat test
```

## 관련 문서

- [정적 데이터 API와 SheetExporter 사용법](Docs/Guide/StaticData.md)
- [서버 시간 처리](Docs/Guide/ServerTime.md)
- [배포 점검 항목](Docs/ETC/DeploymentChecklist.md)
- [설계 결정](Docs/ETC/DesignDecisions.md)

배포 자동화 스크립트는 [deploy.ps1](deploy.ps1)입니다.
