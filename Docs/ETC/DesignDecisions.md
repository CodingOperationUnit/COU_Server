# 방향 결정

서버 작업에서 여러 영역에 걸치는 방향을 정한 내용을 모은다. 결정마다 이유, 검토한 대안, 영향을 함께 적는다.

## 1. 인증된 요청에서 playerId를 얻는 방식
- 결정일: 2026-10-07
- 상태: 결정, 코드 미반영
- 결정: 로그인할 때 발급하는 JWT에 `playerId` claim을 넣는다. subject는 지금처럼 accountId로 둔다. 컨트롤러는 `@AuthenticationPrincipal AuthAccount`에서 playerId를 꺼낸다.

### 이유
- Account와 PlayerProfile은 1:1이다(`player_profile.account_id` unique). 계정이 생긴 뒤 playerId는 바뀌지 않으므로 토큰에 넣어도 실제 값과 어긋나지 않는다.
- `AccountService.signup`이 한 트랜잭션에서 PlayerProfile까지 만들므로 로그인 시점에 playerId가 항상 있다.
- `AuthAccount`가 이미 컨트롤러까지 전달되므로 필드만 추가하면 된다. 별도 리졸버나 공통 클래스가 필요 없다.
- 게임 API는 거의 모두 playerId 기준이다. 요청마다 PlayerProfile을 조회하지 않는다.

### 검토한 대안
- 요청마다 `PlayerProfileRepository.findByAccount_AccountId`로 조회: 계정 코드를 바꾸지 않아도 되지만, 서비스마다 같은 조회가 반복되거나 이를 없앨 리졸버를 따로 만들어야 한다.

### 변경 위치
| 위치 | 변경 |
|---|---|
| `AccountService.login` | PlayerProfile을 조회해 playerId를 얻는다 |
| `JwtProvider.createAccessToken` | `playerId` claim을 추가한다 |
| `JwtProvider.parseToken` | `playerId` claim을 읽어 `AuthAccount`에 넣는다 |
| `AuthAccount` | `Long playerId` 필드를 추가한다 |
| `InventoryController` | `TEMP_PLAYER_ID` 대신 `authAccount.getPlayerId()`를 쓴다 |

### 주의
- 반영 전에 발급된 토큰에는 `playerId` claim이 없다. 반영 후 다시 로그인해야 한다.
- 프로필 생성(`ff53bda`) 이전 코드로 만든 계정은 프로필이 없어 로그인에 실패한다. 로컬 DB를 초기화하거나 로그인에서 오류로 처리한다.
- 계정당 캐릭터가 여러 개가 되면 이 방식은 맞지 않는다. 그때는 캐릭터를 선택할 때 토큰을 다시 발급하는 구조로 바꾼다.

## 2. 정적 데이터의 원천과 전달 방식
- 결정일: 2026-10-07
- 상태: 결정, 코드 미반영
- 관련: `BackendIntegration_DevAkasha.md` 3.6, 4-5
- 결정
  - 구글 시트가 기준이고, 시트에서 생성한 JSON 파일이 원천이다.
  - 서버는 시작할 때 JSON을 읽어 `Map`으로 메모리에 보관하고, 검증과 계산에 쓴다.
  - 서버와 함께 쓰는 테이블은 클라이언트가 접속할 때 서버가 내려주고, 클라이언트는 받은 데이터를 쓴다.
  - 클라이언트 전용 테이블(연출용 등)은 서버를 거치지 않고 클라이언트 빌드에 남긴다.
  - 서버가 내려주는 테이블도 클라이언트 빌드에 사본을 남긴다. 서버에서 받으면 사본을 덮어쓴다.

### 이유
- 클라이언트가 서버 사본을 받아 쓰므로 서버와 클라이언트의 데이터가 어긋나지 않는다.
- 데이터를 바꿀 때 서버만 다시 배포하면 된다. 클라이언트 업데이트가 필요 없다.
- 정적 데이터는 읽기 전용이고 배열 필드가 많다. 메모리에 두면 JSON 구조를 그대로 쓰고, 동기화 로직이 필요 없다.
- 서버 인스턴스마다 같은 파일을 읽으므로 스케일아웃해도 결과가 같다.
- 빌드 사본은 서버 없이 전투 씬만 실행하는 디버그 경로(`WaveManager.stageId` 기본값)에 쓴다.

### 검토한 대안
- DB 적재(지금 `ItemDataLoader` 방식): 테이블마다 엔티티, 리포지토리, 로더가 필요하다. 배열 필드를 별도 테이블로 나눠야 하고, 수정을 반영하려면 동기화 로직이 필요하다.
- Redis: 서버마다 같은 파일을 읽으면 되므로 공유 저장소의 이점이 없고, 조회마다 네트워크를 거친다.
- 서버가 시트를 바로 읽기: 편집 중인 값이 운영에 들어가고, 인스턴스와 클라이언트 사이에 데이터가 어긋날 수 있으며, 서버 시작이 외부 서비스에 묶인다.
- 클라이언트 빌드에만 포함하고 서버는 같은 JSON을 복사: 복사를 빠뜨리면 데이터가 어긋나고, 데이터를 바꿀 때 클라이언트 업데이트가 필요하다.

### 구현 기준
- 클라이언트에는 원본 JSON 내용을 그대로 보낸다. 서버 record는 서버가 쓰는 필드만 가지므로 `Map`을 다시 직렬화하면 클라이언트 전용 필드가 빠진다.
- 클라이언트는 받은 데이터를 로컬에 저장하고, 접속할 때 데이터 버전을 보낸다. 서버는 버전이 다를 때만 데이터를 보낸다.
- 로그인 전에 받으므로 이 API는 인증 없이 호출할 수 있게 연다(`permitAll`).
- 클라이언트 `JsonDataManager`는 지금 `Awake`에서 `Resources`를 동기로 읽는다. 빌드 사본으로 먼저 채우고, 서버 응답을 받으면 덮어쓰는 흐름으로 바꾼다.

### 주의
- 롤링 배포 중에는 데이터 버전이 다른 서버가 함께 돈다. 전투 입장과 결과 검증이 서로 다른 버전에서 처리될 수 있다.

### 미정
- Item: `Equipment.item`이 Item 엔티티를 참조하므로 DB 적재를 유지할지
- 시트에서 JSON을 만드는 절차(수작업 / 빌드 자동화)
- 서버 JSON 위치(jar 포함 / 외부 저장소)
- 데이터 버전 값의 형식
- `application.properties`의 `game.initial.energy`, `game.first-stage-id`와 정적 데이터 중 어느 쪽을 쓸지

## 3. 서버 시각 기준
- 결정일: 2026-10-07
- 상태: 결정, 코드 미반영
- 결정
  - 저장, 계산, 전송을 UTC 시점(`Instant`)으로 통일한다.
  - 현재 시각은 `Clock.systemUTC()` 빈에서 얻는다(`Instant.now(clock)`). DB의 `NOW()`는 쓰지 않는다.
  - DB 저장 기준은 `spring.jpa.properties.hibernate.jdbc.time_zone=UTC`로 고정한다.
  - 응답의 시각은 ISO-8601 UTC 형식(`2026-10-07T03:00:00Z`)으로 보낸다.
  - 서버 시각은 모든 응답에 헤더로 싣는다. DTO마다 `serverTime` 필드를 넣지 않는다.
  - 한국 날짜 경계가 필요한 계산(일일 초기화 등)에서만 `Asia/Seoul`로 변환한다.
  - 공통 인프라는 위 설정, `Clock` 빈, 서버 시각 헤더까지다. 시간 계산 로직(스태미나 회복, 전투 시간 검증, 세션 만료)은 각 도메인 서비스에 두고, 공통 시간 유틸 클래스는 만들지 않는다.

### 이유
- `LocalDateTime`은 시간대 정보가 없어 JVM 기본 시간대에 따라 값이 달라진다. 개발 PC(KST)와 컨테이너·클라우드(UTC)에서 기록한 값이 같은 컬럼에 섞인다.
- 스태미나 회복, 전투 시간 검증, 세션 만료는 모두 경과 시간 계산이다. `Instant`와 `Duration.between`을 쓰면 시간대와 무관하다.
- `Instant`와 `hibernate.jdbc.time_zone=UTC`를 함께 쓰면 JVM 시간대 설정(`-Duser.timezone`)이 필요 없다.
- `Clock` 빈에서 시각을 얻으면 테스트에서 시각을 고정할 수 있다.
- 클라이언트는 메인 화면의 스태미나 회복을 같은 공식으로 표시한다(`BackendIntegration_DevAkasha.md` 3.1). 기기 시계는 틀릴 수 있으므로 서버 시각으로 차이를 보정한다.

### 검토한 대안
- `LocalDateTime` 유지 + JVM 시간대를 UTC로 고정: 고칠 코드는 적지만 모든 실행 환경에 설정을 빠뜨리지 않아야 한다. 응답에 `Z`가 없어 UTC로 해석한다는 약속을 클라이언트와 따로 정해야 한다.
- DTO마다 `serverTime` 필드: 모든 응답 DTO에 같은 필드를 반복해 넣어야 한다.

### 변경 위치
| 위치 | 변경 |
|---|---|
| `application.properties` | `spring.jpa.properties.hibernate.jdbc.time_zone=UTC`를 추가한다 |
| `config` 패키지 | `Clock.systemUTC()`를 빈으로 등록한다 |
| 서버 시각 헤더 | Tomcat의 `Date` 헤더를 쓰거나, 필터로 `X-Server-Time` 헤더를 붙인다 |
| `Account`, `Currency`, `Equipment` | `LocalDateTime` 필드를 `Instant`로 바꾼다. 엔티티 안에서 `now()`를 부르지 않고, 서비스가 `Instant.now(clock)`으로 만든 시각을 넘긴다. 각 작성자와 협의한다 |
| `AccountResponse`, `CurrencyResponse` | 시각 필드를 `Instant`로 바꾼다 |

### 주의
- 서버가 여러 대면 전투 입장과 결과를 서로 다른 인스턴스가 처리할 수 있다. 서버 시계는 NTP로 맞추고, 전투 시간 검증에는 몇 초의 허용 오차를 둔다.
- 기존 로컬 DB의 시각 값은 실행 환경의 시간대(대개 KST)로 들어가 있다. 반영 후 UTC로 읽으면 9시간 어긋나므로 로컬 DB를 초기화한다.

### 확인 필요
- Tomcat이 응답에 `Date` 헤더를 붙이는지, `Instant`가 `Z`가 붙은 ISO-8601 문자열로 나가는지: 서버를 띄우고 `curl -i`로 응답을 확인한다. `Date` 헤더가 없거나 밀리초가 필요하면 `X-Server-Time` 필터를 쓴다.
