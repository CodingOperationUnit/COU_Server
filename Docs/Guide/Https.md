# HTTPS 적용 가이드

EC2 서버(`3.34.14.51`)에 DuckDNS 도메인과 HTTPS/WSS를 적용하는 절차다. WebGL 빌드를 HTTPS 페이지(itch.io, GitHub Pages 등)에 올리면 브라우저가 `http://` API와 `ws://` 채팅을 차단(Mixed Content)하기 때문에 필요하다. PC·모바일 빌드도 같은 주소를 쓴다.

## 1. 구성
| 위치 | 역할 |
|---|---|
| `Caddyfile` | `DOMAIN`의 인증서를 자동 발급·갱신하고, 모든 요청을 `localhost:8080`(Spring)으로 넘긴다 |
| `docker-compose.yaml`의 `caddy` | Caddy 컨테이너. `https` 프로필이라 `.env`에 `COMPOSE_PROFILES=https`가 있을 때만 뜬다 (로컬 개발에는 영향 없음) |
| `.env`의 `DOMAIN` | 인증서를 받을 도메인 |

- Spring 서버 코드는 바꾸지 않는다. Caddy가 같은 서버(127.0.0.1)에서 `X-Forwarded-For`를 붙이므로 `server.forward-headers-strategy=native`로 실제 사용자 IP가 복원된다.
- Caddy는 접속 로그를 남기지 않는다. 브라우저 채팅 연결의 `?token=`이 로그에 남지 않는다.

## 2. 적용 절차

### 2.1 DuckDNS 도메인 만들기
1. https://www.duckdns.org 에 로그인한다.
2. 서브도메인(예: `cou-server`)을 추가한다.
3. `current ip`를 `3.34.14.51`로 바꾸고 `update ip`를 누른다. 탄력적 IP라 IP 자동 갱신 설정은 필요 없다.
4. 확인: `nslookup cou-server.duckdns.org` 결과가 `3.34.14.51`이다.

### 2.2 보안 그룹 열기
AWS 콘솔에서 EC2 보안 그룹 인바운드 규칙을 추가한다.

| 포트 | 소스 | 용도 |
|---|---|---|
| 80 | 0.0.0.0/0 | 인증서 발급 확인, HTTP → HTTPS 이동 |
| 443 | 0.0.0.0/0 | HTTPS, WSS |

### 2.3 EC2에 파일 올리기
서버 레포 루트에서 실행한다.

```powershell
scp -i $env:USERPROFILE\.ssh\cou-key.pem docker-compose.yaml Caddyfile ec2-user@3.34.14.51:~/cou/
```

### 2.4 EC2의 .env 설정과 실행
```bash
ssh -i ~/.ssh/cou-key.pem ec2-user@3.34.14.51
cd ~/cou
echo "COMPOSE_PROFILES=https" >> .env
echo "DOMAIN=cou-server.duckdns.org" >> .env   # 2.1에서 만든 도메인
docker compose up -d
docker logs -f cou-caddy   # "certificate obtained successfully" 가 나오면 Ctrl+C
```

- `COMPOSE_PROFILES`가 `.env`에 있으므로 이후 `deploy.ps1`의 `docker compose up -d`도 Caddy를 함께 띄운다.

### 2.5 확인
```bash
curl -i https://cou-server.duckdns.org/api/static-data   # 200
curl -I http://cou-server.duckdns.org/                    # 308, Location: https://...
```

### 2.6 클라이언트 주소 바꾸기
`COU_Client`의 `GameConstants.Server.BASE_URL`(빌드용 값)을 `https://cou-server.duckdns.org`로 바꾼다. 채팅 주소는 `ServerChatManager`가 `https://`를 `wss://`로 바꿔 만든다.

## 3. 8080 포트
- 이전 빌드가 `http://3.34.14.51:8080`을 쓰므로, 모든 빌드가 HTTPS 주소로 바뀔 때까지 8080을 열어 둔다.
- 8080을 닫으면 `deploy.ps1`의 마지막 외부 접속 확인(`http://3.34.14.51:8080`)이 실패로 표시된다.

## 4. 문제 해결
| 증상 | 확인할 것 |
|---|---|
| Caddy 로그에 인증서 발급 실패 | 보안 그룹 80/443, DuckDNS IP, 다른 프로세스가 80/443을 쓰는지(`sudo ss -ltnp \| grep -E ':80 \|:443 '`) |
| `docker logs cou-caddy`가 컨테이너 없음 | `.env`의 `COMPOSE_PROFILES=https`, `docker compose config --services`에 `caddy`가 있는지 |
| 502 Bad Gateway | Spring 서버가 떠 있는지(`curl -i localhost:8080/api/static-data`) |
