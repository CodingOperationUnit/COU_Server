# 서버 시각 활용 가이드

서버와 클라이언트에서 현재 시각을 얻고 시간을 계산하는 방법을 정리한다. 기준은 `DesignDecisions.md` 3번이다.

## 1. 기본 규칙
- 시각은 UTC 시점(`Instant`)으로 저장, 계산, 전송한다.
- 현재 시각은 `Clock` 빈에서 얻는다.
- 일일 초기화처럼 한국 날짜 경계가 필요할 때만 `Asia/Seoul`로 변환한다.
- 시간 계산 로직은 각 도메인 서비스에 둔다. 공통 시간 유틸 클래스는 만들지 않는다.

## 2. 서버 구성
| 위치 | 역할 |
|---|---|
| `config/ClockConfig` | `Clock.systemUTC()`를 빈으로 등록한다 |
| `config/ServerTimeFilter` | 모든 응답에 `X-Server-Time` 헤더를 붙인다 |
| `application.properties` | `hibernate.jdbc.time_zone=UTC`로 DB 저장 기준을 UTC로 고정한다 |

## 3. 서버에서 쓰기

### 3.1 현재 시각 얻기
서비스에서 `Clock`을 주입받아 `Instant.now(clock)`을 부른다.

```java
@Service
@RequiredArgsConstructor
public class BattleService {
    private final Clock clock;

    public void enter(...) {
        Instant now = Instant.now(clock);
        ...
    }
}
```

다음은 쓰지 않는다.

| 쓰지 않는 것 | 이유 |
|---|---|
| `LocalDateTime.now()` | 시간대 정보가 없어 JVM 기본 시간대에 따라 값이 달라진다 |
| `Instant.now()` | 값은 맞지만 테스트에서 시각을 고정할 수 없다 |
| DB `NOW()` | DB 서버 시간대를 따르고, 테스트에서 시각을 고정할 수 없다 |

### 3.2 엔티티에 시각 넣기
엔티티 안에서 `now()`를 부르지 않는다. 서비스가 만든 시각을 생성자나 메서드 인자로 넘긴다.

```java
// 엔티티
public void recordLogin(Instant now) {
    this.accountLastLoginAt = now;
}

// 서비스
account.recordLogin(Instant.now(clock));
```

### 3.3 경과 시간 계산
스태미나 회복, 전투 시간 검증, 세션 만료는 `Duration.between`으로 계산한다. 시간대와 무관하다.

```java
Duration elapsed = Duration.between(enteredAt, Instant.now(clock));
long elapsedSeconds = elapsed.getSeconds();
```

전투 입장과 결과는 서로 다른 서버가 처리할 수 있다. 전투 시간 검증에는 서버 간 시계 차이를 고려해 몇 초의 허용 오차를 둔다.

### 3.4 한국 날짜 기준 계산
날짜 경계가 필요한 계산에서만 `Asia/Seoul`로 바꾼다. 저장하는 값은 다시 `Instant`로 바꾼다.

```java
private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

LocalDate today = Instant.now(clock).atZone(SEOUL).toLocalDate();
boolean resetToday = lastResetAt.atZone(SEOUL).toLocalDate().equals(today);
Instant nextReset = today.plusDays(1).atStartOfDay(SEOUL).toInstant();
```

### 3.5 테스트에서 시각 고정
고정된 `Clock`을 서비스 생성자에 넘긴다. 시간이 지난 상황은 `Clock.offset`으로 만든다.

```java
Clock fixed = Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC);
Clock tenMinutesLater = Clock.offset(fixed, Duration.ofMinutes(10));
```

## 4. 응답 형식
- 응답 본문의 시각 필드는 `Instant`로 두고, ISO-8601 UTC 문자열로 보낸다(`2026-10-07T03:00:00Z`).
- DTO에 `serverTime` 필드를 넣지 않는다. 서버 시각은 `X-Server-Time` 헤더로 보낸다.
- `X-Server-Time` 값은 요청이 서버에 도착한 시각이다. 형식은 ISO-8601 UTC이고, 소수 초는 0·3·6·9자리 중 하나로 나온다(`2026-10-07T03:00:00.123456Z`).

## 5. 클라이언트에서 쓰기
기기 시계는 틀릴 수 있다. 응답을 받을 때마다 서버 시각과 기기 시각의 차이를 구해 두고, 현재 서버 시각이 필요할 때 기기 시각에 더한다.

```csharp
// 응답을 받을 때마다 갱신한다 (UnityWebRequest 기준)
string header = request.GetResponseHeader("X-Server-Time");
DateTimeOffset serverTime = DateTimeOffset.Parse(header, CultureInfo.InvariantCulture);
serverTimeOffset = serverTime - DateTimeOffset.UtcNow;

// 추정 서버 시각
DateTimeOffset ServerNow => DateTimeOffset.UtcNow + serverTimeOffset;
```

- 응답 본문의 시각 필드도 `DateTimeOffset.Parse`로 읽는다. `DateTime.Parse`는 값을 로컬 시간대로 바꾼다.
- 스태미나 회복 표시는 `ServerNow - currencyEnergyUpdatedAt`으로 경과 시간을 구하고, 서버와 같은 공식으로 계산한다.
- 클라이언트 계산은 표시에만 쓴다. 차감, 지급, 판정에는 서버 응답 값을 쓴다.

## 6. 주의
- `X-Server-Time`은 요청 도착 시각이므로 처리 시간과 네트워크 지연만큼 실제보다 이르다. 초 단위 정확도가 필요한 판정에 쓰지 않는다.
- 사용자가 기기 시계를 바꾸면 저장해 둔 차이가 틀어진다. 다음 응답에서 다시 구한다.
- 서버가 여러 대면 서버 시계를 NTP로 맞춘다.

## 7. 현재 반영 상태
- 반영: `Clock` 빈, `X-Server-Time` 헤더, `hibernate.jdbc.time_zone=UTC`
- 미반영
  - `Account`, `Currency`, `Equipment`의 `LocalDateTime` 필드와 `AccountResponse`, `CurrencyResponse`. 작성자와 협의한 뒤 `Instant`로 바꾼다. 그전까지 이 필드는 `Z` 없이 나간다.
  - `JwtProvider.createAccessToken`은 `Instant.now()`를 직접 부른다.
- 기존 로컬 DB의 시각 값은 KST로 들어가 있어 지금 설정으로 읽으면 9시간 어긋난다. 로컬 DB를 초기화한다.
