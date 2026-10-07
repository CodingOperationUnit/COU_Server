# 백엔드 연동 작업 - DevAkasha 작업분

DevAkasha가 작성한 코드에서 백엔드를 붙일 때 바꿔야 하는 곳을 모은다. 작성자는 `git blame` 기준이다. 값의 권위(`SERVER_AUTH`, `CLIENT_AUTH`)는 `GameData_Tags.md`의 태그를 따르고, 서버 테이블과 필드 이름은 데이터 정의서(Notion)의 동적 데이터 정의서를 따른다.

## 1. 전제
- 서버로 바뀌는 계층은 `LocalLoginManager`, `LocalSaveLoadManager`, `PlayerDataManager`다. 다른 작성자의 코드라서 여기서는 DevAkasha 코드가 이들을 호출하는 곳만 다룬다.
- 지금은 모든 호출이 동기다(`bool` 반환 + `out message`). 서버 요청은 응답을 기다려야 하므로, 요청을 보내는 UI마다 요청 중 상태(중복 입력 막기, 대기 표시)와 통신 실패 처리가 필요하다.
- 데이터 정의서 Currency의 원칙: 클라이언트가 값을 보내 재화를 바꾸지 않는다. 서버가 행동을 검증한 뒤 직접 계산해 저장한다. 그래서 클라이언트에서 재화·경험치·장비를 바꾸는 코드는 모두 "요청 → 응답으로 받은 상태 반영"으로 바뀐다.
- 전투 중 로직(웨이브, 스폰, 드롭, 루팅, 전투 레벨업, 스킬 선택)은 `SESSION` · `CLIENT_AUTH`라 전투 중에는 서버를 호출하지 않는다. 서버를 호출하는 곳은 전투 입장과 전투 결과 두 군데다.

## 2. 필요한 API
| API | 호출 위치 | 현재 처리 | 서버 처리 | 응답 |
|---|---|---|---|---|
| 전투 입장 | `BattleTab.StartBattle` | `PlayerInventory.TrySpendStamina`가 currencyEnergy를 차감하고 저장한다 | 스테이지 해금 검증, 스태미나 회복 계산, battleStaminaCost 차감, 전투 ID 발급 | 전투 ID, Currency |
| 전투 결과 | `BattleManager.EndBattle` → `PlayerInventory.ClaimBattleResult` | `BattleResult.Last`에 담아 두고 메인 씬에서 클라이언트가 지급한다 | 결과 검증, 골드·계정 경험치 지급, 계정 레벨업, StageProgress 갱신, 보상상자 장비 지급 | PlayerProfile, Currency, StageProgress, 지급한 Inventory 행 |
| 로그아웃 | `SettingsPopup.Logout` | `LocalLogin.Logout`이 플레이어 JSON을 저장하고 데이터를 해제한다 | 인증 해제 | 결과 |

## 3. 영역별 작업

### 3.1 전투 입장
- `BattleTab.StartBattle`
  - `TrySpendStamina` 대신 전투 입장 요청을 보내고, 성공 응답을 받은 뒤 `ChangeScene`을 호출한다.
  - 실패 사유(스태미나 부족, 해금되지 않은 스테이지, 통신 실패)를 안내한다. 지금은 스태미나 부족만 처리한다.
  - 응답의 Currency를 `PlayerSaveData`에 반영하고 `NotifyPlayerDataChanged`로 TopBar를 갱신한다.
- `PlayerInventory.TrySpendStamina`는 서버가 차감하므로 삭제 대상이다. `currencyEnergyUpdatedAt`도 서버가 갱신한다.
- 스태미나 회복
  - 회복 코드가 없다. 서버가 `currencyEnergyUpdatedAt` 이후 지난 시간으로 계산한다(데이터 정의서 Currency의 "시간 경과 충전 계산용").
  - 회복 주기가 정해지지 않았다(`GameData_Tags.md` 5.3, 6.6). AccountConst에 열을 추가해야 한다.
  - 서버가 시간으로 회복시키면 TopBar의 스태미나는 마지막 응답 시점 값에 멈춘다. 메인 화면에서 회복을 보여주려면 클라이언트가 같은 공식으로 표시하거나 주기적으로 다시 받아야 한다.
- 스테이지 해금 검증: 데이터 정의서 StageProgress 기준으로 "첫 스테이지 ~ maxClearedStageId 다음 스테이지"만 입장할 수 있다. 지금은 `StageSelectScreen`에서 잠긴 스테이지도 고를 수 있고 입장도 막지 않는다.
- `GameSceneManager.pendingStageId`는 전투 씬에 stageId만 넘긴다. 결과 요청에 실으려면 전투 ID도 함께 넘겨야 한다.
- `WaveManager.stageId` 기본값(`DEBUG`)으로 메인 씬 없이 전투 씬을 실행하면 전투 ID가 없다. 이때는 결과를 보내지 않는 경로가 필요하다.

### 3.2 전투 결과
- `BattleManager.EndBattle`이 만드는 `BattleResult`의 값별 처리

  | 값 | 처리 |
  |---|---|
  | StageId | 서버가 전투 ID로 알 수 있다 |
  | Victory, Seconds, Kills, Gold, RewardBoxes | `CLIENT_AUTH`. 그대로 보내고 서버가 검증한다 |
  | AccountExp | 클라이언트 계산값을 보내지 않는다. 서버가 같은 공식으로 계산한다 |

- 계정 경험치 공식은 `킬 수 × accountExpPerKill + 생존 초 × accountExpPerSecond + 승리 시 clearAccountExp`다. accountExpPerKill, accountExpPerSecond가 `BattleManager` 인스펙터에 있는 임시값이라 서버가 읽을 수 있게 시트로 옮겨야 한다.
- 서버가 같은 정적 테이블을 가지면 다음 상한으로 검증할 수 있다.
  - Seconds: 서버가 기록한 입장 시각부터 결과 수신 시각까지
  - Kills: 스테이지 웨이브(`Wave.json`, `SpawnPattern.json`)에서 Seconds까지 스폰되는 수
  - Gold: 드롭 테이블(`DropTable.json`, `DropItem.json`)의 최대 골드와 행운열차 최대 골드
  - RewardBoxes: 엘리트·보스 스폰 수 × 드롭 테이블의 RewardBox count
- 전송 시점과 유실
  - 지금은 결과를 정적 필드 `BattleResult.Last`에 두고 메인 씬의 `PlayerInventory.Start`에서 지급한다. 그 사이에 앱을 종료하면 결과가 사라진다.
  - 결과창(`BattleResultWindow`)을 띄울 때나 확인 버튼을 누를 때 보낸다. 실패하면 다시 보내야 하고, 같은 전투 ID로 두 번 지급하지 않게 서버가 막아야 한다.
  - 결과를 보내지 못한 전투(강제 종료, 통신 실패)는 서버에 입장 기록만 남는다. 만료 규칙이 필요하다.
- 결과창
  - `BattleResultWindow.SetExp`는 클라이언트 계산값을 보여준다. 서버 계산값과 다를 수 있으므로 응답을 받은 뒤 표시할지 정해야 한다.
  - `SetChapter`, `SetBestTime`은 `HUDTestDriver`에서만 호출한다. 최고 기록은 서버가 내려줘야 한다(3.4).
- 항복(`HomePopup.Exit` → `BattleManager.Surrender`)은 패배 결과를 만들므로 따로 처리할 것이 없다.

### 3.3 보상 지급 (`PlayerInventory.ClaimBattleResult`)
클라이언트가 하던 처리를 모두 서버로 옮긴다. 보상 지급이라 `SERVER_AUTH` 대상이다(`GameData_Tags.md` 5.11).

| 현재 클라이언트 처리 | 서버 처리 |
|---|---|
| currencyGold에 Gold를 더한다 | 검증한 Gold를 더한다 |
| accountExp에 AccountExp를 더하고 레벨업 루프를 돈다 | 서버가 계산한 경험치로 같은 루프를 돈다. 필요 경험치는 `AccountConstData.GetRequiredExp`, 최대 레벨에서는 경험치만 쌓인다 |
| currentStageId를 StageId로 바꾸고, 승리했고 더 높으면 maxClearedStageId를 갱신한다 | 같은 규칙 |
| 보상상자마다 `StageData.RollRewardBoxGrade`로 등급을 뽑고, 그 등급이 기본 등급인 장비 중 균등 랜덤으로 고른다 | 같은 규칙으로 뽑아 Inventory에 넣는다 |
| `CreateNewItem`이 로컬 inventoryId(최대값 + 1)와 획득 시각을 부여한다 | Auto Increment와 서버 시각을 쓴다 |

- 클라이언트는 응답의 PlayerProfile, Currency, StageProgress를 `PlayerSaveData`에 덮어쓰고, 지급된 Inventory 행을 `OwnedItem`으로 바꿔(`PlayerInventory.Load`와 같은 변환) 목록에 넣은 뒤 `RewardBoxResultPopup`을 띄운다.
- 데이터 정의서와 맞출 것
  - PlayerProfile.accountExp 비고가 "저장 방식(누적 / 현재 레벨 기준) 확인 필요"다. 코드는 현재 레벨 기준이다(레벨업할 때 차감).
  - PlayerProfile.accountLevel 비고가 "레벨업 처리 미구현"이다. 클라이언트에는 구현돼 있으므로 서버에 같은 규칙을 구현한다.

### 3.4 스테이지 기록
- 최장 생존 시간을 저장하지 않는다. `ClaimBattleResult`에 "팀 결정 대기" 주석이 있다.
- `BattleTab`은 "최장 생존시간"을 표시하지만 `MainUIAccountBinder`가 항상 0을 넘기고, `StageSelectScreen`이 받는 `StageRecordSaveData.bestSurvivalSeconds`도 0이다.
- 데이터 정의서 StageProgress에 "스테이지별 기록이 필요해지면 별도 StageRecord 테이블로 분리한다"고 돼 있다. 테이블을 추가하거나 UI에서 뺀다.

### 3.5 계정
- `LogInMainCanvas.TryLogin`은 아이디를 정규화(Trim, 소문자)한 뒤 `Login` → `LoadPlayerAfterLogin`을 차례로 호출한다. 서버에서는 로그인 응답에 플레이어 데이터를 함께 받거나 두 요청을 이어서 보낸다. 서버도 같은 정규화 규칙을 써야 한다.
- `SettingsPopup.Logout`은 지금 저장에 실패하면 로그아웃을 막는다. 서버 연동 후에는 저장 단계가 없어지고 인증 해제와 로컬 데이터 해제만 남는다.

### 3.6 정적 데이터 (`JsonDataManager`, `Data/*`)
- 서버 검증에 필요한 테이블: AccountConst(경험치 곡선, 최대 레벨, 스태미나), Stage(clearAccountExp, rewardBoxGradeWeights, waveId), Wave, SpawnPattern, Monster, DropTable, DropItem, Item(보상상자 후보와 기본 등급)
- 서버와 클라이언트가 같은 버전의 테이블을 써야 한다. 지금 `JsonDataManager`는 `Awake`에서 `Resources`의 JSON을 동기로 읽는다. 배포 방식을 정해야 한다.
  - 빌드에 포함: 지금 구조를 유지하고, 접속할 때 테이블 버전을 비교한다
  - 서버에서 받기: 로그인 전에 받아 `JsonDataManager`에 넣는 단계가 필요하다
- 시트의 AccountConst 탭에 battleStaminaCost 열이 없다(`GameData_Tags.md` 6.8). 서버가 시트에서 테이블을 만든다면 먼저 열을 추가해야 한다.
- stageIllustrationColor는 연출용이라 서버에는 필요 없다.

### 3.7 클라이언트에만 남는 것
- 테마(`UIManager`, PlayerPrefs `UITheme`)와 음소거(`SettingsPopup`, `PauseWindow`, 저장하지 않음)는 `CLIENT_PREF`라 서버가 필요 없다.
- 전투 중 로직: `BattleManager`의 전투 레벨업·스킬 선택, `WaveManager`, `MonsterSpawner`, `DropItemManager`와 드롭 ECS 시스템, `Enemy`·`Box`의 드롭 테이블 처리
- UI 프레임워크: `UIManager`, `UIView`, `UIPopup`, `SceneUIRoot`, `SafeArea`, 테마 관련 클래스

## 4. 정해야 할 것
1. 통신 방식(코루틴, async, 콜백)과 요청 중 UI 처리
2. 전투 결과 전송 시점과 결과창에 표시할 값(클라이언트 계산값 / 서버 응답값)
3. 결과를 보내지 못한 전투의 만료 규칙
4. `CLIENT_AUTH` 값의 검증 범위(상한 검증 / 그대로 신뢰)
5. 정적 데이터 배포 방식과 버전 확인
6. 스태미나 회복 주기와 메인 화면 표시 방식
7. 최장 생존 시간 저장 여부(StageRecord 테이블)
8. accountExp 저장 방식(누적 / 현재 레벨 기준)

## 5. 범위에서 뺀 부분
- 진화 노드 해금(`EvolutionTab` 외), 도전 보상 수령(`ChallengeTab` 외): 구현 여부를 아직 정하지 않았다
- 다른 작성자의 코드: `LocalLoginManager`, `LocalSaveLoadManager`, `SaveLoadHelper`, `PlayerDataManager`, `PlayerSaveData`(로그인·저장 계층), `PlayerInventory`의 장착·강화·합성·재화 메서드, 상점 카드, `PlayerLuckTrain`
- `MainUIAccountBinder`는 `PlayerSaveData`를 화면에 표시만 한다. 응답을 `PlayerSaveData`에 반영하고 `NotifyPlayerDataChanged`를 호출하면 그대로 동작한다.
