# 배포 체크리스트

## 공통
- [ ] `DEV_TOOLS_ENABLED`가 꺼져 있다. 켜져 있으면 기동 로그에 dev 도구 WARN이 찍힌다 (보석 상품 무료 지급, dev API 노출)
- [ ] `JWT_SECRET`이 개발용 값이 아니다

## 네트워크
- [ ] 외부에 여는 포트는 HTTPS(80, 443)와 서버 포트(기본 8080)뿐이다. MySQL(3306), Redis(6379)는 열지 않는다
    - HTTPS는 Caddy가 처리한다 ([HTTPS 적용 가이드](../Guide/Https.md)). 모든 빌드가 HTTPS 주소를 쓰면 8080은 닫아도 된다
    - compose는 두 포트를 127.0.0.1에만 연다 (`docker ps`의 PORTS가 `127.0.0.1:`로 시작하는지 확인)
    - AWS에서는 보안 그룹에서도 3306, 6379를 막는다. 관리형 Redis(ElastiCache)를 쓰면 AUTH와 보안 그룹을 따로 설정한다
- [ ] `REDIS_PASSWORD`가 설정되어 있다 (없으면 compose가 실행을 거부한다)
- [ ] `server.forward-headers-strategy=native`가 켜져 있다 (기본값)
    - 같은 서버의 Nginx, AWS ALB, ngrok 같은 터널은 내부망 주소라 설정 없이 실제 사용자 IP가 복원된다
    - Cloudflare 프록시처럼 **공인 IP를 가진 프록시**를 앞에 두면 신뢰할 프록시를 따로 지정해야 한다
      (`server.tomcat.remoteip.internal-proxies`). 지정하지 않으면 모든 사용자가 프록시 IP로 묶여 함께 차단된다

## 로그인 잠금 해제 (운영·테스트 중 필요할 때)
- 계정 잠금: `docker exec -it cou-redis redis-cli -a {REDIS_PASSWORD} --no-auth-warning DEL auth:login:fail:{loginId 소문자}`
- IP 제한: `docker exec -it cou-redis redis-cli -a {REDIS_PASSWORD} --no-auth-warning DEL auth:ip:{IP}`
- 임계값은 `.env`의 `AUTH_*` 값으로 조정한다

## 전투·재화 검증 설정
- [ ] `DEV_TOOLS_ENABLED`가 꺼져 있다. 켜져 있으면 전투 시간 검증(최소 승리 시간, 시간 비례 상한)도 꺼진다
- [ ] `BATTLE_MAX_SPEED_MULTIPLIER`는 정식 배속 기능이 없으면 1.0이다
- [ ] 스키마는 `ddl-auto=update`로 자동 생성한다. 이 방식은 기존 컬럼 타입을 바꾸지 않으므로, 값이 늘어날 수 있는 enum 컬럼(`suspicious_activity.activity_type`, `currency_ledger.currency_type`, `currency_ledger.reason`)은 varchar로 고정되어 있다

## 의심 활동과 재화 조회
계정별 의심 활동 집계:
```sql
SELECT player_id, activity_type, COUNT(*) AS cnt, MAX(created_at) AS last_at
FROM suspicious_activity
GROUP BY player_id, activity_type
ORDER BY cnt DESC;
```

한 계정의 최근 재화 변동:
```sql
SELECT created_at, currency_type, delta, balance_after, reason, ref_id
FROM currency_ledger
WHERE player_id = ?
ORDER BY id DESC
LIMIT 50;
```

골드 상한의 95% 이상을 계속 받는 계정 (최근 7일, 10판 이상):
```sql
SELECT player_id, COUNT(*) AS battles,
       SUM(gold_limit > 0 AND gold >= gold_limit * 0.95) AS near_gold_limit
FROM battle_record
WHERE created_at >= UTC_TIMESTAMP() - INTERVAL 7 DAY
GROUP BY player_id
HAVING battles >= 10 AND near_gold_limit / battles >= 0.8
ORDER BY near_gold_limit DESC;
```