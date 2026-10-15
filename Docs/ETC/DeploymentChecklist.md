# 배포 체크리스트

## 공통
- [ ] `DEV_TOOLS_ENABLED`가 꺼져 있다. 켜져 있으면 기동 로그에 dev 도구 WARN이 찍힌다 (보석 상품 무료 지급, dev API 노출)
- [ ] `JWT_SECRET`이 개발용 값이 아니다

## 네트워크
- [ ] 외부에 여는 포트는 서버 포트(기본 8080)뿐이다. MySQL(3306), Redis(6379)는 열지 않는다
- [ ] `server.forward-headers-strategy=native`가 켜져 있다 (기본값)
    - 같은 서버의 Nginx, AWS ALB, ngrok 같은 터널은 내부망 주소라 설정 없이 실제 사용자 IP가 복원된다
    - Cloudflare 프록시처럼 **공인 IP를 가진 프록시**를 앞에 두면 신뢰할 프록시를 따로 지정해야 한다
      (`server.tomcat.remoteip.internal-proxies`). 지정하지 않으면 모든 사용자가 프록시 IP로 묶여 함께 차단된다

## 로그인 잠금 해제 (운영·테스트 중 필요할 때)
- 계정 잠금: `docker exec -it cou-redis redis-cli DEL auth:login:fail:{loginId 소문자}`
- IP 제한: `docker exec -it cou-redis redis-cli DEL auth:ip:{IP}`
- 임계값은 `.env`의 `AUTH_*` 값으로 조정한다