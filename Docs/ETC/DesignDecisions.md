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
  - Item도 다른 테이블과 같이 메모리에 둔다. 지금의 DB 적재는 임시이고, 정적 데이터 캐싱을 구현한 뒤 후속 작업으로 바꾼다(아래 "후속 작업: Item").

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

### 세부 결정

#### 데이터 생성 절차: 원터치 자동화
- 클라이언트의 `Tools > Google Sheets > JSON Exporter`를 확장한다. 버튼 하나로 시트의 모든 탭을 내보내 클라이언트 `Assets/Resources/JsonFiles`와 서버 레포 `src/main/resources/data/`에 함께 저장한다.
- 서버 레포의 로컬 경로는 EditorPrefs에 설정한다.
- 도구는 파일 생성까지만 한다. 커밋은 사람이 diff를 확인하고 한다.
- 서버 `data/`에 저장하는 탭: AccountConst, Stage, Wave, SpawnPattern, Monster, DropTable, DropItem, Item(`BackendIntegration_DevAkasha.md` 3.6)
- 이유: 시트 접근(Apps Script, TSV 다운로드)과 형식 검증이 이미 있다. 한 번에 양쪽에 저장하므로 복사를 빠뜨리지 않는다.
- Exporter는 jyj8943 작성 코드라서 확장은 작성자와 협의한다.

#### 서버 JSON 위치: jar 포함
- `src/main/resources/data/`에 두고 jar에 넣는다.
- 이유: 코드와 데이터가 한 빌드로 묶여, 같은 빌드의 인스턴스는 같은 데이터를 쓴다. 외부 저장소는 다시 읽는 로직과 저장소 운영이 필요하다.

#### 데이터 버전: `0.0.0` 3단계
| 자리 | 올리는 경우 |
|---|---|
| 첫째 | 클라이언트 코드를 바꿔야 하는 변경(클라이언트가 읽는 열의 삭제·이름·자료형 변경, 클라이언트가 새로 써야 하는 열 추가) |
| 둘째 | 테이블·행 추가 |
| 셋째 | 값 수정 |

- 묶음 전체에 버전 하나를 둔다.
- 원터치 도구가 서버 `data/`에 버전 파일을 함께 쓴다. 서버 `data/`의 기존 파일과 내용이 다르면 셋째 자리를 자동으로 올린다. 첫째·둘째 자리는 내보낼 때 사람이 고르고, 아랫자리는 0으로 돌린다.
- 클라이언트 빌드는 자신이 읽을 수 있는 첫째 자리 값을 가진다. 서버 버전의 첫째 자리가 다르면 앱 업데이트를 요구한다.
- 이유: 사람이 올리기를 빠뜨리면 내용이 달라도 버전이 같아 클라이언트가 새 데이터를 받지 않는다. 그래서 내용이 바뀌면 도구가 최소 셋째 자리를 올린다.

#### 초기값: 정적 데이터 사용
- 초기 재화는 AccountConst의 initialGold, initialGem, initialStamina를 쓴다. `application.properties`의 `game.initial.*`는 지운다.
- 첫 스테이지는 Stage의 최소 stageId로 정한다. `game.first-stage-id`는 지운다.
- 이유: 지금 `application.properties`와 AccountConst에 같은 값이 두 번 정의돼 있다. 스테이지 해금 규칙(첫 스테이지 ~ maxClearedStageId 다음 스테이지)이 이미 stageId 순서를 쓴다.
- `PlayerService`는 jyj8943 작성 코드라서 작성자와 협의한다.

### 주의
- 롤링 배포 중에는 데이터 버전이 다른 서버가 함께 돈다. 전투 입장과 결과 검증이 서로 다른 버전에서 처리될 수 있다.
- 자동화 전에 시트 AccountConst 탭에 battleStaminaCost 열을 추가한다. 그대로 내보내면 열이 사라져 전투 입장 스태미나 비용이 0이 된다(`GameData_Tags.md` 6.8).
- 첫째 자리가 바뀌는 변경은 클라이언트 업데이트가 필요하다. "서버만 다시 배포하면 된다"(이유 2)는 둘째·셋째 자리 변경에만 해당한다.

### 후속 작업: Item
- 시점: 정적 데이터 캐싱을 구현한 뒤. 인벤토리 작성자가 바꾼다.
- 지금 DB에 둔 이유: 장비가 Item을 FK로 참조하고, 장착 슬롯 조회와 합성에서 item을 조인한다. 캐싱 전까지의 임시 처리다(작성자 확인).

#### 조인 없이 처리하는 방법
- 합성: itemId 값만 비교하고 Item의 다른 열은 쓰지 않는다. 장비 행의 itemId로 조회하면 된다.
- 장착 슬롯 조회: 장착 중인 장비는 최대 6개다. 모두 가져와 정적 데이터의 slotType으로 거르면 된다. 쿼리 수는 지금과 같다.
- FK 무결성: 장비는 서버만 만들고(보상, 상점, 합성) itemId를 정적 데이터에서 고르므로, 만들 때 보장된다.

#### 변경 위치
| 위치 | 변경 |
|---|---|
| `Equipment` | `@ManyToOne Item item`을 `Long itemId`(`item_id` 열)로 바꾼다. `create`는 정적 데이터의 Item 행에서 기본 등급을 얻는다 |
| `EquipmentRepository` | `findByPlayerIdAndItemSlotTypeAndEquippedTrue`를 `findByPlayerIdAndEquippedTrue`로 바꾼다. 합성 쿼리의 `Item_Id`를 `ItemId`로 바꾼다 |
| `InventoryService` | 장착할 때 장착 중인 장비를 가져와 정적 데이터의 slotType으로 같은 슬롯의 장비를 찾는다 |
| `getItem().getId()`를 쓰는 곳(`InventoryService`, `InventoryItemResponse`, 합성 응답) | `getItemId()`로 바꾼다 |
| `Item`, `ItemRepository`, `ItemDataLoader`, `ItemDataFile` | 삭제한다. `ItemData`는 정적 데이터의 Item record로 옮긴다 |

#### 주의
- 바꾸기 전까지 `ItemDataLoader`는 이미 있는 ID를 건너뛴다. Item 값을 바꾸면 로컬 DB를 초기화해야 서버에 반영된다.
- `ddl-auto=update`는 `item` 테이블과 `equipment.item_id` FK를 지우지 않는다. 바꾼 뒤 로컬 DB를 초기화한다.
- 보유 장비가 itemId를 참조하므로 Item 시트에서 행을 지우지 않는다.

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
