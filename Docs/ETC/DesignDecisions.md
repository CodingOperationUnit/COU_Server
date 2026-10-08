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

## 4. 전투 입장과 결과
- 결정일: 2026-10-08
- 상태: 결정, 서버 코드 반영(시트 열 추가는 dev 머지 때). 클라이언트 연동은 `Docs/Guide/Battle.md`로 전달
- 관련: `BackendIntegration_DevAkasha.md` 2, 3.1~3.4, 4-2·3·4·7·8
- 결정
  - 입장할 때 서버가 전투 세션을 DB에 만들고 전투 ID를 발급한다. 결과는 전투 ID로 보낸다.
  - 플레이어당 진행 중인 전투는 하나다. 새로 입장하면 이전 전투를 만료시킨다.
  - 결과는 결과창을 열 때 보낸다. 경험치, 골드, 레벨업, 보상상자는 응답값으로 표시한다.
  - `CLIENT_AUTH` 값은 스테이지 상한으로 검증하고, 넘으면 상한까지만 지급한다.
  - 계정 경험치는 서버가 계산하고, 현재 레벨 기준으로 저장한다.
  - 최장 생존 시간은 StageRecord 테이블에 저장한다.

### 이유
- 재화, 경험치, 장비는 서버가 행동을 검증한 뒤 직접 계산해 저장한다(데이터 정의서 Currency 원칙). 클라이언트 계산값은 받지 않는다.
- 서버가 여러 대면 입장과 결과를 서로 다른 인스턴스가 처리할 수 있다. 세션을 DB에 두면 어느 인스턴스든 같은 세션을 읽는다.
- 늦게 보낸 결과로 더 얻는 것이 없으므로 시간으로 만료시킬 이유가 없다. 새로 입장할 때만 만료시키면 시간 기준과 정리 작업이 필요 없다.
- 결과창을 열 때 보내면 결과창에서 앱을 꺼도 결과가 남는다. 보상상자 등급은 서버가 뽑으므로 어차피 응답을 기다려야 한다.
- 스테이지 상한은 서버가 이미 가진 정적 데이터로 미리 계산할 수 있어 구현이 단순하고, 터무니없는 값은 막는다.
- 현재 레벨 기준은 클라이언트 코드와 같아서 변환이 필요 없다.
- 최장 생존 시간은 UI(`BattleTab`, `StageSelectScreen`, `BattleResultWindow.SetBestTime`)가 이미 있고, 결과 API를 어차피 만들므로 저장하는 비용이 작다.

### 검토한 대안
- 세션을 서버 메모리에 저장: 입장과 결과를 다른 인스턴스가 받으면 세션을 찾지 못한다.
- 시간 만료: 일시정지와 백그라운드 때문에 여유를 크게 잡아야 하고, 그 값을 따로 정해야 한다.
- 확인 버튼에서 전송: 결과창에서 앱을 끄면 결과가 사라지고, 결과창에 보인 값과 실제 지급값이 다를 수 있다.
- 시간별 상한 검증: Seconds 시점까지 스폰되는 수로 검증해 더 정확하지만, 서버가 웨이브 타임라인을 재현해야 한다.
- 그대로 신뢰: 조작을 막지 못한다.
- 상한을 넘으면 거부: 클라이언트 오차 하나로 결과 전체를 잃고, 다시 보내도 계속 실패한다.
- accountExp 누적 저장: 클라이언트 레벨업 코드를 바꿔야 하고, 경험치 곡선을 바꾸면 기존 플레이어의 레벨이 바뀐다.
- 최장 생존 시간을 UI에서 제거: 이미 만든 UI를 버린다.

### 세부 결정

#### 전투 입장
- 서버 처리 순서: 스테이지 해금 검증 → 스태미나 회복 계산(5번) → battleStaminaCost 차감 → 진행 중인 전투 만료 → 세션 생성
- 해금 범위: 첫 스테이지(Stage의 최소 stageId)부터 maxClearedStageId 다음 stageId까지
- 실패 사유: 스태미나 부족, 해금되지 않은 스테이지
- 응답: 전투 ID, Currency

#### 전투 세션
| 값 | 용도 |
|---|---|
| 전투 ID | 결과 요청 식별, 중복 지급 방지 |
| playerId, stageId | 요청자 확인, 스테이지 정적 데이터 조회 |
| 입장 시각 | Seconds 검증 |
| 상태(진행 중, 완료, 만료) | 중복 지급 방지, 만료 |

- 진행 중인 세션에만 지급한다. 완료된 세션으로 다시 받으면 지급하지 않는다.
- 만료된 전투는 보상을 주지 않고, 스태미나도 돌려주지 않는다.

#### 결과 전송과 표시
- `BattleManager.EndBattle`에서 결과창(`BattleResultWindow`)을 띄우면서 보낸다. 실패하면 같은 전투 ID로 다시 보낸다.
- 보내는 값: 전투 ID, Victory, Seconds, Kills, Gold, RewardBoxes. StageId와 AccountExp는 보내지 않는다.
- 결과창은 킬 수와 생존 시간을 바로 표시하고, 경험치, 골드, 레벨업, 보상상자는 응답을 받은 뒤 표시한다.
- 응답: PlayerProfile, Currency, StageProgress, StageRecord, 지급한 Inventory 행, 지급한 골드와 경험치

#### `CLIENT_AUTH` 검증
| 값 | 상한 |
|---|---|
| Seconds | 입장 시각부터 결과 수신 시각까지. 서버 간 시계 차이만큼 허용 오차를 둔다(3번) |
| Kills | 스테이지 웨이브의 전체 스폰 수(상자 제외) |
| Gold | 스폰되는 몬스터의 드롭 테이블 최대 골드 합 + 행운상자 최대 개수 × luckTrainGoldMax × 5(최대 당첨 칸 수) |
| RewardBoxes | 스폰되는 몬스터의 드롭 테이블 RewardBox 최대 개수 합 |

- Kills, Gold, RewardBoxes의 상한은 서버를 시작할 때 스테이지별로 계산해 메모리에 둔다.
- 넘으면 상한까지만 지급하고 로그를 남긴다.

#### 지급
- 골드: 검증한 Gold를 더한다.
- 계정 경험치: `Kills × accountExpPerKill + Seconds × accountExpPerSecond + 승리 시 clearAccountExp`. 검증한 값으로 계산한다.
  - accountExpPerKill, accountExpPerSecond는 `BattleManager` 인스펙터에서 AccountConst 열로 옮긴다.
  - 현재 레벨 기준으로 저장한다. 레벨업하면 필요 경험치를 빼고, 최대 레벨에서는 경험치만 쌓인다. 필요 경험치는 클라이언트 `AccountConstData.GetRequiredExp`와 같은 공식을 쓴다.
- StageProgress: currentStageId를 stageId로 바꾸고, 승리했고 더 높으면 maxClearedStageId를 갱신한다.
- 보상상자: 상자마다 Stage의 rewardBoxGradeWeights로 등급을 뽑고, 그 등급이 기본 등급인 장비 중 균등 랜덤으로 골라 Equipment로 넣는다.
- StageRecord: (playerId, stageId)마다 bestSurvivalSeconds를 둔다. 승패와 관계없이 Seconds가 더 길면 갱신하고, 세이브 로드 응답에도 싣는다.

### 변경 위치
클라이언트 변경 위치는 `BackendIntegration_DevAkasha.md` 3.1~3.3을 따른다.

| 위치 | 변경 |
|---|---|
| battle 도메인 (신규) | 입장·결과 API, 전투 세션 엔티티, StageRecord 엔티티, 스테이지별 상한 계산 |
| 시트 AccountConst 탭, `AccountConstData` | accountExpPerKill, accountExpPerSecond, luckTrainGoldMax 열을 추가한다 |
| `CurrencyRepository` | 입장·결과에서 플레이어 단위로 차례대로 처리하도록 행 잠금 조회를 추가한다 |
| `PlayerProfile` | 경험치 지급과 레벨업 메서드를 추가한다 |
| `StageProgress` | 전투 결과 반영 메서드를 추가한다 |
| `PlayerSaveDataResponse`, `PlayerService.loadSave` | StageRecord 목록을 추가한다. jyj8943 작성 코드라서 작성자와 협의한다 |

### 주의
- 결과를 보내기 전에 앱을 종료하면 결과가 사라지고, 다음 입장 때 그 전투는 만료된다.
- `WaveManager.stageId` 기본값으로 메인 씬 없이 실행하면 전투 ID가 없다. 이때는 결과를 보내지 않는다.
- 데이터 버전이 다른 서버가 함께 돌면(2번 주의) 입장과 결과 검증의 상한이 다를 수 있다.
- 보상상자 장비는 `Equipment.create(playerId, ItemData)`로 만든다.
- `PUT /api/players/me/save`(`PlayerService`, jyj8943 작성)는 클라이언트가 보낸 재화, 경험치, 레벨, 스테이지를 그대로 저장한다. 외부에서 이 경로를 부르면 위 검증을 거치지 않으므로 작성자에게 전달한다.

### 확인 결과 (2026-10-08)
- 행운열차 최대 골드: `PlayerLuckTrain`은 행운상자 하나마다 `Random(goldMin, goldMax) × 당첨 칸 수(1/3/5)`를 준다. goldMax를 AccountConst `luckTrainGoldMax`로 옮기고, 행운상자 최대 개수(드롭 테이블) × luckTrainGoldMax × 5를 Gold 상한에 더한다.
  - 결과에 행운상자 개수를 받지 않는다. 개수도 상한까지 부풀릴 수 있어 최악의 상한이 같다.
- 완료된 전투 ID로 다시 받은 결과: `BATTLE_ALREADY_COMPLETED`(409)로 응답하고 지급 결과를 다시 주지 않는다.
  - 이 경우는 첫 요청이 반영됐는데 응답만 잃은 드문 경우다. 지급분은 DB에 있으므로 클라이언트가 세이브와 인벤토리를 다시 받으면 맞춰진다. 잃는 것은 결과창의 지급값 표시뿐이다.
  - 중복 지급은 Currency 행과 세션 행을 잠가 막는다. 첫 요청을 처리하는 중에 재전송이 와도 차례대로 처리되어 늦은 요청은 완료 상태를 본다.
- Victory 하한: 검증하지 않는다.

### 확인 필요
- 데이터 정의서(Notion) 갱신: StageRecord 테이블 추가, PlayerProfile.accountExp·accountLevel 비고

## 5. 스태미나 회복
- 결정일: 2026-10-08
- 상태: 결정, 서버 코드 반영(시트 열 추가는 dev 머지 때)
- 관련: `BackendIntegration_DevAkasha.md` 3.1, 4-6, 이 문서 3번
- 결정
  - 300초마다 1씩 회복한다. AccountConst에 `staminaRecoverySeconds` 열을 추가한다.
  - 회복을 따로 저장하지 않는다. 스태미나를 쓰거나 내려줄 때 `currencyEnergyUpdatedAt` 이후 지난 시간으로 계산해 반영한다.
  - maxStamina 이상이면 회복하지 않는다.
  - 회복하고 남은 시간은 버리지 않고 이어서 쓴다.
  - 클라이언트는 메인 화면에서 같은 공식과 서버 시각(3번)으로 회복을 표시만 한다.

### 계산
```
if energy >= maxStamina:
    updatedAt = now
else:
    recovered = floor((now - updatedAt) / staminaRecoverySeconds)
    energy = min(maxStamina, energy + recovered)
    updatedAt = (energy >= maxStamina) ? now : updatedAt + recovered × staminaRecoverySeconds
```
- 차감은 회복을 계산한 뒤에 한다. 최대치에서 차감하면 그 시점부터 회복이 시작된다.

### 이유
- 마지막 갱신 시각과 현재 시각만 있으면 언제 계산해도 같은 값이 나온다. 회복할 때마다 저장할 필요가 없다.
- 남은 시간을 버리면 자주 접속할수록 회복이 늦어진다.
- 300초면 60을 다 채우는 데 5시간, 전투 1회(battleStaminaCost 5) 분량을 채우는 데 25분이다.

### 검토한 대안
- 360초: 60을 다 채우는 데 6시간, 전투 1회 분량을 채우는 데 30분이다.

### 변경 위치
| 위치 | 변경 |
|---|---|
| 시트 AccountConst 탭, `AccountConstData` | `staminaRecoverySeconds` 열을 추가한다 |
| `Currency` | 회복 계산과 차감 메서드를 추가한다. 시각은 서비스가 `Instant.now(clock)`으로 넘긴다 |

### 주의
- `Currency.currencyEnergyUpdatedAt`을 3번에 따라 `Instant`로 바꿨다. 기존 로컬 DB 값은 KST로 들어가 있으므로 초기화한다(3번 주의).
- 세이브 로드(`PlayerService.loadSave`)는 읽기 전용이라 회복을 응답에만 반영하고 저장하지 않는다. 같은 공식이라 다음에 계산해도 결과가 같다.

## 6. 클라이언트 통신 방식
- 결정일: 2026-10-08
- 상태: 결정, 코드 미반영
- 관련: `BackendIntegration_DevAkasha.md` 1, 4-1
- 결정
  - 서버 요청은 코루틴과 콜백으로 쓴다. `JsonDataManager.FetchStaticData`(`IEnumerator`, `Action<bool>`)와 같은 방식이다.
  - 요청을 보내는 UI마다 요청 중 상태를 둔다. 중복 입력을 막고 대기 표시를 띄우며, 실패하면 사유를 안내한다.

### 이유
- 기존 통신 코드와 방식이 같고, 패키지를 추가할 필요가 없다.

### 검토한 대안
- async/await(Unity 6 `Awaitable`): 요청을 이어서 보낼 때(로그인 후 로드) 코드가 간단하지만, 기존 `FetchStaticData`와 방식이 갈린다.
