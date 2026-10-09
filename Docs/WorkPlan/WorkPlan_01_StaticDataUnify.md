# WorkPlan 01: 정적 데이터 경로 통일

## 목표
- 정적 데이터를 내려주는 경로 3개를 `GET /api/static-data` 하나로 합친다.
- 정적 데이터는 `StaticDataService` 한 곳에서 메모리로 관리한다(`Docs/ETC/DesignDecisions.md` 2번).

| 현재 경로 | 처리 |
|---|---|
| `GET /api/static-data` | 유지. 테이블 12개 → 13개(PlayerBaseStat 추가) |
| `GET /api/master/versions`, `/api/master/{tableName}` | 제거. PlayerBaseStat은 시트 탭으로 옮겨 static-data로 내려준다 |
| `GET /api/master/enemy/{tableName}` | 제거. 4개 테이블은 이미 static-data에 있으므로 monster 패키지에만 있는 검증을 옮긴다 |

## 범위
- 포함
  - 구글 시트에 PlayerBaseStat 탭을 추가하고 SheetExporter로 Export한다
  - PlayerBaseStat: DB 적재를 없애고 메모리로 옮긴다
  - monster 패키지에만 있는 검증(MonsterAttack, SpawnPattern, Wave)을 `StaticDataService`로 옮긴다
  - `master`, `monster` 패키지를 삭제하고 `SecurityConfig` 규칙을 정리한다
  - `Docs/Guide/StaticData.md`를 갱신한다
- 제외
  - 클라이언트(COU_client) 수정
  - SheetExporter 코드 수정
  - ItemConst의 서버 record와 검증
  - Skill, Shop(이미 record와 검사가 있음)
  - 남아 있는 `data/BossAttack.json`(어디서도 읽지 않음)

## 작업 항목
경로는 `src/main/java/com/couserver/`, `src/main/resources/` 기준이다.

| # | 작업 단위 | 산출물 | 의존 |
|---|---|---|---|
| 1 | 시트에 `PlayerBaseStat` 탭을 추가하고 Export한다(아래 1 목록) | `data/PlayerBaseStat.json`, `data/tables.txt`, `data/version.txt` | - |
| 2 | `PlayerBaseStatData` record를 staticdata로 옮긴다. 필드는 그대로 두고 패키지만 바꾼다 | `staticdata/dto/PlayerBaseStatData.java` | - |
| 3 | `MonsterAttackData` record와 `MonsterAttackType` enum을 추가한다(아래 3 목록) | `staticdata/dto/MonsterAttackData.java`, `staticdata/dto/MonsterAttackType.java` | - |
| 4 | `StaticDataService`를 수정한다(아래 4-1~4-3) | `staticdata/service/StaticDataService.java` | #1, #2, #3 |
| 5 | `PlayerFinalStatService`에서 `PlayerBaseStatRepository`를 없애고 `staticDataService.getPlayerBaseStat()`로 읽는다. getter는 record 접근자(`playerBaseAttack()` 등)로 바꾼다 | `player/service/PlayerFinalStatService.java` | #4 |
| 6 | monster 패키지를 삭제한다(아래 6 목록) | 삭제 | #4 |
| 7 | master 패키지를 삭제한다(아래 7 목록) | 삭제 | #5, #6 |
| 8 | `/api/master/**`의 `permitAll` 규칙을 삭제한다 | `config/SecurityConfig.java` | #7 |
| 9 | 가이드 문서를 갱신한다(아래 9 목록) | `Docs/Guide/StaticData.md` | #4 ~ #8 |
| 10 | 빌드하고 기동해 확인한다(아래 10 목록) | - | #1 ~ #9 |

### 1. 시트 탭 추가와 Export
- 탭 이름은 `PlayerBaseStat`, 데이터 행은 1개다. 값은 지금 `PlayerBaseStat.json`과 같게 둔다.
- 열(2행 필드명 / 3행 자료형): `playerBaseAttack`, `playerBaseHp`, `playerBaseCriticalDamage`, `playerBaseCriticalChance`, `playerBaseSkillDamage`는 `int`, `playerBaseMoveSpeed`, `playerBaseMaxMoveSpeed`, `playerBaseLootRadius`는 `float`
- SheetExporter에서 둘째 자리를 선택해 Export한다(테이블 추가). `version.txt`는 `2.1.0` → `2.2.0`이 된다.
- 결과 확인: `PlayerBaseStat.json`이 `{ "datas": [행 1개] }` 형식이고, `tables.txt` 끝에 `PlayerBaseStat`이 있으며, 그 밖의 JSON은 바뀌지 않았는지 diff로 본다.

### 3. MonsterAttack record
- `MonsterAttackData`: `int monsterAttackId`, `int monsterId`, `MonsterAttackType monsterAttackType`(검사에 쓰는 열만)
- `MonsterAttackType`: `MELEE`, `RANGED`, `AREA`, `TRAP`(시트 값 `Melee` 등은 대소문자 무시로 읽힌다)

### 4. StaticDataService 수정
테이블 목록은 `tables.txt`에서 읽으므로 목록 수정은 없다.
- 4-1. PlayerBaseStat: `playerBaseStat` 필드(`PlayerBaseStatData`)를 추가한다. AccountConst처럼 행이 정확히 1개인지 검사한다.
- 4-2. MonsterAttack: 맵 필드 없이 행을 읽어 검사만 한다. 서버에서 쓰는 곳이 없기 때문이다. `monsters` 맵을 만든 뒤에 검사한다.
  - `requireIds`(monsterAttackId가 1 이상이고 중복 없음)
  - monsterId가 `monsters`에 있음
  - 그 몬스터의 monsterType이 `BOX`가 아님
  - 몬스터의 monsterType이 `BOSS`가 아니면 monsterAttackType이 `MELEE`가 아님
- 4-3. monster 패키지에서 옮기는 검사
  - SpawnPattern: patternDuration ≥ 0, patternDuration > 0(반복 패턴)이면 spawnInterval > 0
  - Wave: patternStartTime ≥ 0

### 6. monster 패키지 삭제 목록 (`monster/`)
- `controller/EnemyMasterController.java`
- `data/` 전체 12개: `MonsterAttackData`, `MonsterAttackDataFile`, `MonsterAttackType`, `MonsterData`, `MonsterDataFile`, `MonsterType`, `SpawnEventType`, `SpawnFormation`, `SpawnPatternData`, `SpawnPatternDataFile`, `WaveEntryData`, `WaveEntryDataFile`
- `table/` 전체 5개: `MonsterAttackTable`, `MonsterTable`, `SpawnPatternTable`, `WaveTable`, `TableFileReader`

### 7. master 패키지 삭제 목록 (`master/`)
- `controller/MasterController.java`
- `service/MasterDataService.java`, `service/PlayerBaseStatDataLoader.java`
- `entity/MasterTableVersion.java`, `entity/PlayerBaseStat.java`
- `repository/MasterTableVersionRepository.java`, `repository/PlayerBaseStatRepository.java`
- `dto/MasterTableResponse.java`, `dto/MasterVersionsResponse.java`, `dto/PlayerBaseStatData.java`(#2에서 옮김)
- `exception/MasterErrorCode.java`(사용처 `EnemyMasterController`는 #6에서 삭제)

### 9. StaticData.md 갱신 항목
- 2절: 표에 Shop, Skill, MonsterAttack, PlayerBaseStat(행 1개)을 추가한다.
- 3절: 예시 버전을 `2.2.0`으로 바꾼다.
- 5.1절: `getShopProducts()`, `getSkills()`, `getPlayerBaseStat()`를 추가하고, 사용처 목록에 `ShopService`, `PlayerFinalStatService`를 추가한다.
- 5.3절: "시트와 무관한 `PlayerBaseStat.json` 같은 파일은 목록에 없으므로 내려가지 않는다" 문장을 삭제한다.
- 5.4절: PlayerBaseStat(행 1개), 4-2, 4-3의 검사를 추가한다. 빠져 있는 Shop, Skill 검사도 함께 적는다.
- 6절: "PlayerBaseStat.json은?" 항목을 삭제한다.

### 10. 확인
- `./gradlew build`: 컴파일과 `CouServerApplicationTests`(컨텍스트 로드)를 확인한다. 로컬 MySQL이 필요하다.
- 기동 후 `curl -i "http://localhost:8080/api/static-data"`: `tables`가 13개이고 `PlayerBaseStat`이 있는지 확인한다.
- `curl -i "http://localhost:8080/api/master/versions"`, `curl -i "http://localhost:8080/api/master/enemy/monster"`: 인증 없이 401이 오는지 확인한다(경로가 닫혔는지).
- 최종 스탯 API(`PlayerStatController`): 기초 스탯 값이 변경 전과 같은지 확인한다.

## 합의된 구현 결정
- 작업 기준: dev(605fe2a)를 머지한 상태에서 진행한다.
- PlayerBaseStat: 시트 탭으로 추가해 `tables.txt`에 올린다(시트가 원천, DesignDecisions 2번). DB 엔티티, 리포지토리, 로더, `master_table_version`을 없앤다.
- monster 검증: MonsterAttack 3종, SpawnPattern 2종, Wave 1종을 `StaticDataService`로 옮긴다. MonsterAttack은 record를 추가한다.
- BossAttack: MonsterAttack으로 대체되었으므로(34b6d8a) 다루지 않는다.
- ItemConst: 지금처럼 원본 전송만 한다.
- `/api/master/**`: 이번 작업에서 제거한다.
- 테이블별 int 버전(`master_table_version`)은 없애고 `version.txt` 하나로 관리한다.
- record에는 서버가 읽는 열만 둔다(기존 규칙). MonsterAttackData는 검사에 쓰는 세 열만 가진다.

## 의존 순서 / 병렬 가능 그룹
- 병렬: #1, #2, #3
- 직렬: (#1, #2, #3) → #4
- #4 다음 병렬: #5, #6
- 직렬: (#5, #6) → #7 → #8 → #9 → #10

## 미해결 / 실행 시 확인 필요
- 클라이언트 전환: 클라이언트가 `/api/master/...`, `/api/master/enemy/...`를 부르는 곳을 static-data `tables`로 바꾸는 작업을 같이 머지해야 한다. 그 전에는 해당 호출이 401로 실패한다. PlayerBaseStat은 master 응답의 `data` 객체에서 `tables.PlayerBaseStat.datas[0]`로 위치가 바뀐다.
- 로컬 DB: `ddl-auto=update`는 `player_base_stat`, `master_table_version` 테이블을 지우지 않는다. 직접 DROP하거나 로컬 DB를 초기화한다.
- Export 영향: 시트와 서버 JSON이 다르면 다른 테이블도 덮어써진다. 특히 AccountConst의 서버 전용 열(StaticData.md 6절)이 시트에 없으면 빠져서 서버가 뜨지 않는다. #1 diff에서 확인한다.
- 작성자 협의: master, monster 패키지를 삭제하기 전에 작성자와 협의할지 확인한다.
