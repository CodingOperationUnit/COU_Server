package com.couserver.battle.service;

import com.couserver.audit.entity.SuspiciousActivityType;
import com.couserver.audit.service.SuspiciousActivityRecorder;
import com.couserver.account.dto.CurrencyResponse;
import com.couserver.account.dto.PlayerProfileResponse;
import com.couserver.account.dto.StageProgressResponse;
import com.couserver.account.entity.Currency;
import com.couserver.account.entity.PlayerProfile;
import com.couserver.account.entity.StageProgress;
import com.couserver.account.repository.CurrencyRepository;
import com.couserver.account.repository.PlayerProfileRepository;
import com.couserver.account.repository.StageProgressRepository;
import com.couserver.achievement.dto.BattleAchievementProgress;
import com.couserver.achievement.service.AchievementService;
import com.couserver.battle.dto.BattleEnterResponse;
import com.couserver.battle.dto.BattleResultRequest;
import com.couserver.battle.dto.BattleResultResponse;
import com.couserver.battle.dto.StageRecordResponse;
import com.couserver.battle.entity.BattleSession;
import com.couserver.battle.entity.BattleStatus;
import com.couserver.battle.entity.StageRecord;
import com.couserver.battle.exception.BattleErrorCode;
import com.couserver.battle.repository.BattleSessionRepository;
import com.couserver.battle.repository.StageRecordRepository;
import com.couserver.common.exception.BusinessException;
import com.couserver.inventory.dto.EquipmentResponse;
import com.couserver.inventory.entity.Equipment;
import com.couserver.inventory.entity.ItemGrade;
import com.couserver.inventory.repository.EquipmentRepository;
import com.couserver.ranking.event.RankingChangedEvent;
import com.couserver.staticdata.dto.AccountConstData;
import com.couserver.staticdata.dto.DropItemType;
import com.couserver.staticdata.dto.DropTableEntryData;
import com.couserver.staticdata.dto.ItemData;
import com.couserver.staticdata.dto.MonsterType;
import com.couserver.staticdata.dto.SpawnPatternData;
import com.couserver.staticdata.dto.StageData;
import com.couserver.staticdata.dto.StageRosterData;
import com.couserver.staticdata.dto.WaveEntryData;
import com.couserver.staticdata.service.StaticDataService;
import com.couserver.ledger.entity.LedgerReason;
import com.couserver.ledger.service.CurrencyLedgerService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BattleService {
    private static final int SECONDS_TOLERANCE = 5;           // 입장과 결과를 다른 서버가 받을 때의 시계 차이 허용(초)
    private static final int LUCK_TRAIN_MAX_PICKS = 5;        // 행운열차 한 번의 최대 당첨 칸 수 (PlayerLuckTrain)
    private static final int SKILL_MAX_LEVEL_UPS = 4;         // 클라이언트 SkillBase.MaxLevel(5)
    private static final Set<DropItemType> GOLD_TYPES =
            EnumSet.of(DropItemType.GOLD1, DropItemType.GOLD2, DropItemType.GOLD3, DropItemType.GOLD4);

    private final StaticDataService staticDataService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final BattleSessionRepository battleSessionRepository;
    private final StageRecordRepository stageRecordRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final CurrencyRepository currencyRepository;
    private final StageProgressRepository stageProgressRepository;
    private final EquipmentRepository equipmentRepository;
    private final AchievementService achievementService;
    private final StageUnlockPolicy stageUnlockPolicy;
    private final CurrencyLedgerService currencyLedger;
    private final SuspiciousActivityRecorder suspiciousActivityRecorder;
    private final BattleTimePolicy battleTimePolicy;

    private Map<Integer, StageLimit> stageLimits;           // key: stageId, 스테이지 전체 기준 상한
    private Map<Integer, Double> bossSpawnSeconds;          // key: stageId, 보스가 처음 나오는 시각(초). 보스가 없는 스테이지는 없음          // key: stageId
    private Map<ItemGrade, List<ItemData>> itemsByGrade;    // 보상상자 장비 후보

    // 스테이지 한 판에서 클라이언트가 보낼 수 있는 값의 상한
    private record StageLimit(int kills, int gold, int rewardBoxes, Map<DropItemType, Integer> pickups) {
    }

    @PostConstruct
    void computeFromStaticData() {
        stageLimits = staticDataService.getStages().values().stream()
                .collect(Collectors.toUnmodifiableMap(StageData::stageId,
                        stage -> computeStageLimit(stage, Double.POSITIVE_INFINITY)));
        Map<Integer, Double> bossTimes = new HashMap<>();
        for (StageData stage : staticDataService.getStages().values()) {
            computeBossSpawnSeconds(stage).ifPresent(seconds -> bossTimes.put(stage.stageId(), seconds));
        }
        bossSpawnSeconds = Map.copyOf(bossTimes);
        itemsByGrade = Map.copyOf(staticDataService.getItems().values().stream()
                .collect(Collectors.groupingBy(ItemData::grade, Collectors.toUnmodifiableList())));
    }

    // 전투 입장: 해금 검증 → 스태미나 회복 계산 → 차감 → 이전 세션 삭제 → 세션 생성
    @Transactional
    public BattleEnterResponse enter(Long playerId, int stageId) {
        if (!staticDataService.getStages().containsKey(stageId)) {
            throw new BusinessException(BattleErrorCode.STAGE_NOT_FOUND);
        }

        Currency currency = getCurrencyForUpdate(playerId);

        StageProgress progress = getStageProgress(playerId);
        if (!stageUnlockPolicy.isUnlocked(stageId, progress.getMaxClearedStageId())) {
            throw new BusinessException(BattleErrorCode.STAGE_LOCKED);
        }

        AccountConstData accountConst = staticDataService.getAccountConst();
        Instant now = Instant.now(clock);
        currency.recoverEnergy(now, accountConst.maxStamina(), accountConst.staminaRecoverySeconds());
        if (currency.getCurrencyEnergy() < accountConst.battleStaminaCost()) {
            throw new BusinessException(BattleErrorCode.NOT_ENOUGH_STAMINA);
        }
        currency.spendEnergy(accountConst.battleStaminaCost());

        battleSessionRepository.deleteByPlayerId(playerId);
        BattleSession session = battleSessionRepository.save(new BattleSession(playerId, stageId, now));

        return new BattleEnterResponse(session.getBattleId(), new CurrencyResponse(currency));
    }

    // 전투 결과: 진행 중인 세션에만 지급한다. 클라이언트 값은 상한까지만 인정하고, 경험치와 보상상자 장비는 서버가 정한다
    @Transactional
    public BattleResultResponse submitResult(Long playerId, Long battleId, BattleResultRequest request) {
        Currency currency = getCurrencyForUpdate(playerId);

        BattleSession session = battleSessionRepository.findForUpdate(battleId)
                .orElseThrow(() -> new BusinessException(BattleErrorCode.BATTLE_NOT_FOUND));
        if (!session.getPlayerId().equals(playerId)) {
            throw new BusinessException(BattleErrorCode.NOT_OWNED_BATTLE);
        }
        if (session.getStatus() == BattleStatus.COMPLETED) {
            throw new BusinessException(BattleErrorCode.BATTLE_ALREADY_COMPLETED);
        }

        int stageId = session.getStageId();
        StageData stage = staticDataService.getStages().get(stageId);

        List<String> overLimits = new ArrayList<>();   // 상한을 넘은 값. 전투 하나에 의심 기록 하나로 남긴다
        long elapsedSeconds = Duration.between(session.getEnteredAt(), Instant.now(clock)).getSeconds();
        // 서버 기준 경과 시간 동안 전투에서 흐를 수 있는 최대 게임 시간. 상한도 그 시각까지 나온 몬스터로 계산한다
        double gameSeconds = battleTimePolicy.maxGameSeconds(elapsedSeconds + SECONDS_TOLERANCE);
        StageLimit limit = limitAt(stage, gameSeconds);

        // 승리는 보스 처치뿐이고 보스는 등장 시각 전에는 나오지 않으므로, 그보다 짧은 시간에 끝난 승리는 패배로 처리한다
        boolean tooFastVictory = request.victory() && isTooFastVictory(stageId, gameSeconds);
        boolean victory = request.victory() && !tooFastVictory;

        int seconds = clamp(overLimits, "seconds", request.seconds(), (int) Math.min(gameSeconds, Integer.MAX_VALUE));
        int kills = clamp(overLimits, "kills", request.kills(), limit.kills());
        int gold = clamp(overLimits, "gold", request.gold(), limit.gold());
        int rewardBoxes = clamp(overLimits, "rewardBoxes", request.rewardBoxes(), limit.rewardBoxes());

        AccountConstData accountConst = staticDataService.getAccountConst();
        int exp = kills * accountConst.accountExpPerKill()
                + seconds * accountConst.accountExpPerSecond()
                + (victory ? stage.clearAccountExp() : 0);

        PlayerProfile profile = playerProfileRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "플레이어 데이터가 없습니다."));
        profile.gainExp(exp, accountConst);
        currencyLedger.addGold(currency, gold, LedgerReason.BATTLE_RESULT, battleId);

        // 업적 진행 누적 (같은 트랜잭션)
        achievementService.addBattleProgress(playerId,
                buildAchievementProgress(overLimits, request, victory, kills, gold, limit, exp));

        if (!overLimits.isEmpty()) {
            suspiciousActivityRecorder.record(playerId, SuspiciousActivityType.BATTLE_RESULT_OVER_LIMIT,
                    "battleId=" + battleId + " stageId=" + stageId + " " + String.join(", ", overLimits));
        }
        if (tooFastVictory) {
            suspiciousActivityRecorder.record(playerId, SuspiciousActivityType.BATTLE_RESULT_TOO_FAST,
                    "battleId=" + battleId + " stageId=" + stageId + " elapsedSeconds=" + elapsedSeconds
                            + " bossSpawnSeconds=" + bossSpawnSeconds.get(stageId));
        }

        StageProgress progress = getStageProgress(playerId);
        progress.recordBattle(stageId, victory);

        StageRecord record = stageRecordRepository.findByPlayerIdAndStageId(playerId, stageId)
                .orElseGet(() -> new StageRecord(playerId, stageId));
        record.recordSurvival(seconds);
        stageRecordRepository.save(record);

        List<Equipment> rewards = equipmentRepository.saveAll(rollRewardBoxes(playerId, stage, rewardBoxes));

        session.complete();
        // 랭킹값 변경알림(Redis에 갱신)
        eventPublisher.publishEvent(new RankingChangedEvent(playerId));
        return new BattleResultResponse(
                gold,
                exp,
                new PlayerProfileResponse(profile),
                new CurrencyResponse(currency),
                new StageProgressResponse(progress),
                new StageRecordResponse(record),
                rewards.stream()
                        .map(e -> new EquipmentResponse(
                                e.getId(), e.getItemId(), e.getLevel(), e.getGrade().toClientName(), e.isEquipped()))
                        .toList());
    }

    // 상한을 넘으면 상한까지만 인정하고, 넘은 내용을 모아 둔다 (기록은 submitResult에서 한 번에 남긴다)
    private static int clamp(List<String> overLimits, String name, int value, int max) {
        if (value <= max) {
            return value;
        }
        overLimits.add(name + " client=" + value + " max=" + max);
        return max;
    }

    // 상자마다 스테이지 가중치로 등급을 뽑고, 그 등급이 기본 등급인 장비 중 하나를 균등하게 고른다
    private List<Equipment> rollRewardBoxes(Long playerId, StageData stage, int count) {
        List<Equipment> rewards = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            List<ItemData> pool = itemsByGrade.getOrDefault(rollGrade(stage.rewardBoxGradeWeights()), List.of());
            if (pool.isEmpty()) {
                continue;
            }
            rewards.add(Equipment.create(playerId, pool.get(ThreadLocalRandom.current().nextInt(pool.size()))));
        }
        return rewards;
    }

    // 클라이언트 StageData.RollRewardBoxGrade와 같은 규칙. 가중치 순서는 ItemGrade 순서
    private static ItemGrade rollGrade(List<Integer> weights) {
        int total = weights.stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            return ItemGrade.GENERAL;
        }

        int roll = ThreadLocalRandom.current().nextInt(total);
        for (int i = 0; i < weights.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) {
                return ItemGrade.values()[i];
            }
        }
        return ItemGrade.GENERAL;
    }

    // 그 시각까지 나올 수 있는 몬스터로 계산한 상한. 시간 검증을 건너뛰면(dev 도구) 미리 계산해 둔 전체 상한을 쓴다
    private StageLimit limitAt(StageData stage, double gameSeconds) {
        if (Double.isInfinite(gameSeconds)) {
            return stageLimits.get(stage.stageId());
        }
        return computeStageLimit(stage, gameSeconds);
    }

    private boolean isTooFastVictory(int stageId, double gameSeconds) {
        Double bossAt = bossSpawnSeconds.get(stageId);
        return bossAt != null && gameSeconds < bossAt;
    }

    // 스테이지의 보스(MonsterType.BOSS)가 처음 등장하는 시각(초). 보스가 없는 스테이지는 비어 있다
    private OptionalDouble computeBossSpawnSeconds(StageData stage) {
        Map<String, StageRosterData> roster = staticDataService.getStageRosters().get(stage.stageId());
        return staticDataService.getWaves().get(stage.waveId()).stream()
                .filter(entry -> staticDataService.getMonsters()
                        .get(roster.get(entry.monsterSlot()).monsterId()).monsterType() == MonsterType.BOSS)
                .mapToDouble(WaveEntryData::patternStartTime)
                .min();
    }

    // seconds초까지 나온 웨이브의 모든 스폰을 처치하고, 몬스터 역할별 드롭 테이블마다 가장 많이 나오는 행이 뽑힌 경우를 더한다.
    // seconds가 무한대면 스테이지 전체 기준이다
    private StageLimit computeStageLimit(StageData stage, double seconds)  {
        int kills = 0;
        int gold = 0;
        int rewardBoxes = 0;
        int luckyBoxes = 0;
        Map<DropItemType, Integer> pickups = new EnumMap<>(DropItemType.class);

        Map<String, StageRosterData> roster = staticDataService.getStageRosters().get(stage.stageId());
        for (WaveEntryData entry : staticDataService.getWaves().get(stage.waveId())) {
            StageRosterData rosterRow = roster.get(entry.monsterSlot());
            int patternId = rosterRow.patternId() > 0 ? rosterRow.patternId() : entry.patternId();
            SpawnPatternData pattern = staticDataService.getSpawnPatterns().get(patternId);
            MonsterType monsterType = staticDataService.getMonsters().get(rosterRow.monsterId()).monsterType();
            int spawns = SpawnSchedule.spawnCountBy(entry, pattern, seconds);

            // 상자는 킬 수에 들어가지 않는다 (클라이언트 Box.GiveReward)
            if (monsterType != MonsterType.BOX) {
                kills += spawns;
            }

            List<DropTableEntryData> dropTable = staticDataService.getDropTables().get(monsterType);
            gold += spawns * maxDrop(dropTable, this::goldOf);
            rewardBoxes += spawns * maxDrop(dropTable, row -> countOf(row, DropItemType.REWARDBOX));
            luckyBoxes += spawns * maxDrop(dropTable, row -> countOf(row, DropItemType.LUCKYBOX));

            // 업적 '아이템 획득' 상한: 종류별로 가장 많이 나오는 경우
            for (DropItemType type : DropItemType.values()) {
                pickups.merge(type, spawns * maxDrop(dropTable, row -> countOf(row, type)), Integer::sum);
            }
        }

        // 행운상자 하나마다 행운열차 한 번: goldMax × 당첨 칸 수
        gold += luckyBoxes * staticDataService.getAccountConst().luckTrainGoldMax() * LUCK_TRAIN_MAX_PICKS;
        return new StageLimit(kills, gold, rewardBoxes, Map.copyOf(pickups));
    }


    // 드롭 테이블 한 번의 판정에서 나올 수 있는 최대값.
    // 클라이언트 DropItemManager.SpawnTable처럼 연속된 같은 dropGroup마다 1행을 뽑으므로 그룹별 최대값을 더한다
    private static int maxDrop(List<DropTableEntryData> dropTable, ToIntFunction<DropTableEntryData> value) {
        int total = 0;
        int groupMax = 0;
        for (int i = 0; i < dropTable.size(); i++) {
            DropTableEntryData row = dropTable.get(i);
            if (i > 0 && row.dropGroup() != dropTable.get(i - 1).dropGroup()) {
                total += groupMax;
                groupMax = 0;
            }
            groupMax = Math.max(groupMax, value.applyAsInt(row));
        }
        return total + groupMax;
    }

    private int goldOf(DropTableEntryData row) {
        if (!GOLD_TYPES.contains(row.dropItemType())) {
            return 0;
        }
        return staticDataService.getDropItems().get(row.dropItemType()).value() * row.count();
    }

    private static int countOf(DropTableEntryData row, DropItemType type) {
        return row.dropItemType() == type ? row.count() : 0;
    }

    private Currency getCurrencyForUpdate(Long playerId) {
        return currencyRepository.findForUpdate(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "재화 데이터가 없습니다."));
    }

    private StageProgress getStageProgress(Long playerId) {
        return stageProgressRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "스테이지 데이터가 없습니다."));
    }

    // 업적 집계 검증: 모르는 스킬/아이템은 버리고, 스킬 처치는 인정된 총 처치 수까지, 레벨업·획득은 상한까지만 인정한다
    private BattleAchievementProgress buildAchievementProgress(
            List<String> overLimits, BattleResultRequest request, boolean victory, int kills, int gold, StageLimit limit, int exp) {

        Map<Integer, Integer> skillKills = new HashMap<>();
        int remainingKills = kills;
        for (Map.Entry<Integer, Integer> entry : nullToEmpty(request.skillKills()).entrySet()) {
            if (!isKnownSkill(entry.getKey()) || entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            int count = clamp(overLimits, "skillKills." + entry.getKey(), entry.getValue(), remainingKills);
            if (count > 0) {
                skillKills.put(entry.getKey(), count);
                remainingKills -= count;
            }
        }

        Map<Integer, Integer> skillUpgrades = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : nullToEmpty(request.skillUpgrades()).entrySet()) {
            if (!isKnownSkill(entry.getKey()) || entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            skillUpgrades.put(entry.getKey(),
                    clamp(overLimits, "skillUpgrades." + entry.getKey(), entry.getValue(), SKILL_MAX_LEVEL_UPS));
        }

        // 키는 대소문자를 구분하지 않고 같은 종류로 모인다("Bomb", "bomb", "BOMB").
        // 키마다 상한을 적용한 뒤 더하면 키를 나눠 보내 상한을 몇 배로 늘릴 수 있으므로,
        // 원본 값을 종류별로 먼저 합친 뒤 종류마다 한 번만 상한을 적용한다
        Map<DropItemType, Long> pickupTotals = new EnumMap<>(DropItemType.class);
        Map<DropItemType, Integer> pickupKeyCounts = new EnumMap<>(DropItemType.class);
        for (Map.Entry<String, Integer> entry : nullToEmpty(request.pickups()).entrySet()) {
            DropItemType type = parseDropItemType(entry.getKey());
            if (type == null || entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            pickupTotals.merge(type, entry.getValue().longValue(), Long::sum);   // int 범위를 넘지 않게 long으로 합친다
            pickupKeyCounts.merge(type, 1, Integer::sum);
        }

        Map<DropItemType, Integer> pickups = new EnumMap<>(DropItemType.class);
        for (Map.Entry<DropItemType, Long> entry : pickupTotals.entrySet()) {
            DropItemType type = entry.getKey();
            int keyCount = pickupKeyCounts.get(type);
            if (keyCount > 1) {
                // 정상 클라이언트는 종류마다 키를 하나만 보낸다
                overLimits.add("pickups." + type + " keys=" + keyCount);
            }
            int total = (int) Math.min(entry.getValue(), Integer.MAX_VALUE);
            pickups.put(type, clamp(overLimits, "pickups." + type, total, limit.pickups().getOrDefault(type, 0)));
        }

        // 행운열차는 럭키박스 1개당 최대 1번이라, 인정된 럭키박스 획득 수를 넘을 수 없다
        int luckTrains = clamp(overLimits, "luckTrains", request.luckTrains(),
                pickups.getOrDefault(DropItemType.LUCKYBOX, 0));

        return new BattleAchievementProgress(
                skillKills, skillUpgrades, pickups, exp,
                gold, victory ? 1 : 0, luckTrains);
    }

    private boolean isKnownSkill(Integer skillId) {
        return skillId != null && staticDataService.getSkills().containsKey(skillId.longValue());
    }

    // 클라이언트는 "Bomb"(설정에 따라 "bomb")으로 보내므로 대문자로 맞춘다
    private static DropItemType parseDropItemType(String name) {
        if (name == null) {
            return null;
        }
        try {
            return DropItemType.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static <K> Map<K, Integer> nullToEmpty(Map<K, Integer> map) {
        return map == null ? Map.of() : map;
    }
}
