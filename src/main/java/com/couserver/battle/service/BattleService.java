package com.couserver.battle.service;

import com.couserver.account.dto.CurrencyResponse;
import com.couserver.account.dto.PlayerProfileResponse;
import com.couserver.account.dto.StageProgressResponse;
import com.couserver.account.entity.Currency;
import com.couserver.account.entity.PlayerProfile;
import com.couserver.account.entity.StageProgress;
import com.couserver.account.repository.CurrencyRepository;
import com.couserver.account.repository.PlayerProfileRepository;
import com.couserver.account.repository.StageProgressRepository;
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
import com.couserver.staticdata.dto.AccountConstData;
import com.couserver.staticdata.dto.DropItemType;
import com.couserver.staticdata.dto.DropTableEntryData;
import com.couserver.staticdata.dto.ItemData;
import com.couserver.staticdata.dto.MonsterType;
import com.couserver.staticdata.dto.SpawnPatternData;
import com.couserver.staticdata.dto.StageData;
import com.couserver.staticdata.dto.WaveEntryData;
import com.couserver.staticdata.service.StaticDataService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BattleService {
    private static final int SECONDS_TOLERANCE = 5;           // 입장과 결과를 다른 서버가 받을 때의 시계 차이 허용(초)
    private static final double MIN_SPAWN_INTERVAL = 0.1;     // 클라이언트 WaveManager.minRepeatInterval
    private static final int LUCK_TRAIN_MAX_PICKS = 5;        // 행운열차 한 번의 최대 당첨 칸 수 (PlayerLuckTrain)
    private static final Set<DropItemType> GOLD_TYPES =
            EnumSet.of(DropItemType.GOLD1, DropItemType.GOLD2, DropItemType.GOLD3, DropItemType.GOLD4);

    private final StaticDataService staticDataService;
    private final Clock clock;
    private final BattleSessionRepository battleSessionRepository;
    private final StageRecordRepository stageRecordRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final CurrencyRepository currencyRepository;
    private final StageProgressRepository stageProgressRepository;
    private final EquipmentRepository equipmentRepository;

    private Map<Integer, StageLimit> stageLimits;   // key: stageId

    // 스테이지 한 판에서 클라이언트가 보낼 수 있는 값의 상한
    private record StageLimit(int kills, int gold, int rewardBoxes) {
    }

    @PostConstruct
    void computeStageLimits() {
        stageLimits = staticDataService.getStages().values().stream()
                .collect(Collectors.toUnmodifiableMap(StageData::stageId, this::computeStageLimit));
    }

    // 전투 입장: 해금 검증 → 스태미나 회복 계산 → 차감 → 진행 중인 전투 만료 → 세션 생성
    @Transactional
    public BattleEnterResponse enter(Long playerId, int stageId) {
        if (!staticDataService.getStages().containsKey(stageId)) {
            throw new BusinessException(BattleErrorCode.STAGE_NOT_FOUND);
        }

        Currency currency = getCurrencyForUpdate(playerId);

        StageProgress progress = getStageProgress(playerId);
        if (!isUnlocked(stageId, progress.getMaxClearedStageId())) {
            throw new BusinessException(BattleErrorCode.STAGE_LOCKED);
        }

        AccountConstData accountConst = staticDataService.getAccountConst();
        Instant now = Instant.now(clock);
        currency.recoverEnergy(now, accountConst.maxStamina(), accountConst.staminaRecoverySeconds());
        if (currency.getCurrencyEnergy() < accountConst.battleStaminaCost()) {
            throw new BusinessException(BattleErrorCode.NOT_ENOUGH_STAMINA);
        }
        currency.spendEnergy(accountConst.battleStaminaCost());

        battleSessionRepository.updateStatus(playerId, BattleStatus.IN_PROGRESS, BattleStatus.EXPIRED);
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
        if (session.getStatus() == BattleStatus.EXPIRED) {
            throw new BusinessException(BattleErrorCode.BATTLE_EXPIRED);
        }

        int stageId = session.getStageId();
        StageData stage = staticDataService.getStages().get(stageId);
        StageLimit limit = stageLimits.get(stageId);

        long elapsedSeconds = Duration.between(session.getEnteredAt(), Instant.now(clock)).getSeconds();
        int seconds = clamp(battleId, "seconds", request.seconds(), (int) elapsedSeconds + SECONDS_TOLERANCE);
        int kills = clamp(battleId, "kills", request.kills(), limit.kills());
        int gold = clamp(battleId, "gold", request.gold(), limit.gold());
        int rewardBoxes = clamp(battleId, "rewardBoxes", request.rewardBoxes(), limit.rewardBoxes());

        AccountConstData accountConst = staticDataService.getAccountConst();
        int exp = kills * accountConst.accountExpPerKill()
                + seconds * accountConst.accountExpPerSecond()
                + (request.victory() ? stage.clearAccountExp() : 0);

        PlayerProfile profile = playerProfileRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "플레이어 데이터가 없습니다."));
        profile.gainExp(exp, accountConst);
        currency.addGold(gold);

        StageProgress progress = getStageProgress(playerId);
        progress.recordBattle(stageId, request.victory());

        StageRecord record = stageRecordRepository.findByPlayerIdAndStageId(playerId, stageId)
                .orElseGet(() -> new StageRecord(playerId, stageId));
        record.recordSurvival(seconds);
        stageRecordRepository.save(record);

        List<Equipment> rewards = equipmentRepository.saveAll(rollRewardBoxes(playerId, stage, rewardBoxes));

        session.complete();

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

    // 요청 스테이지보다 앞에 클리어하지 않은 스테이지가 없으면 해금된 것이다 (첫 스테이지 ~ maxClearedStageId 다음 스테이지)
    private boolean isUnlocked(int stageId, Integer maxClearedStageId) {
        return staticDataService.getStages().keySet().stream()
                .noneMatch(id -> id < stageId && (maxClearedStageId == null || id > maxClearedStageId));
    }

    // 상한을 넘으면 상한까지만 인정하고 로그를 남긴다
    private int clamp(Long battleId, String name, int value, int max) {
        if (value <= max) {
            return value;
        }
        log.warn("전투 {}의 {} 값 {}이(가) 상한 {}을(를) 넘어 상한으로 처리합니다.", battleId, name, value, max);
        return max;
    }

    // 상자마다 스테이지 가중치로 등급을 뽑고, 그 등급이 기본 등급인 장비 중 하나를 균등하게 고른다
    private List<Equipment> rollRewardBoxes(Long playerId, StageData stage, int count) {
        Map<ItemGrade, List<ItemData>> itemsByGrade = staticDataService.getItems().values().stream()
                .collect(Collectors.groupingBy(ItemData::grade));

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

    // 웨이브의 모든 스폰을 처치하고, 드롭 테이블마다 가장 많이 나오는 행이 뽑힌 경우를 더한다
    private StageLimit computeStageLimit(StageData stage) {
        int kills = 0;
        int gold = 0;
        int rewardBoxes = 0;
        int luckyBoxes = 0;

        for (WaveEntryData entry : staticDataService.getWaves().get(stage.waveId())) {
            SpawnPatternData pattern = staticDataService.getSpawnPatterns().get(entry.patternId());
            int spawns = maxSpawnCount(pattern);

            // 상자는 킬 수에 들어가지 않는다 (클라이언트 Box.GiveReward)
            if (staticDataService.getMonsters().get(entry.monsterId()).monsterType() != MonsterType.BOX) {
                kills += spawns;
            }
            if (pattern.dropTableId() <= 0) {
                continue;
            }

            List<DropTableEntryData> dropTable = staticDataService.getDropTables().get(pattern.dropTableId());
            gold += spawns * maxDrop(dropTable, this::goldOf);
            rewardBoxes += spawns * maxDrop(dropTable, row -> countOf(row, DropItemType.REWARDBOX));
            luckyBoxes += spawns * maxDrop(dropTable, row -> countOf(row, DropItemType.LUCKYBOX));
        }

        // 행운상자 하나마다 행운열차 한 번: goldMax × 당첨 칸 수
        gold += luckyBoxes * staticDataService.getAccountConst().luckTrainGoldMax() * LUCK_TRAIN_MAX_PICKS;
        return new StageLimit(kills, gold, rewardBoxes);
    }

    // WaveManager와 같은 규칙: patternDuration이 0이면 한 번, 아니면 [시작, 시작 + patternDuration) 동안 간격마다 스폰한다.
    // 클라이언트는 간격을 float로 누적하므로, 나누어떨어질 때 한 번 더 나올 수 있게 floor + 1로 센다
    private static int maxSpawnCount(SpawnPatternData pattern) {
        if (pattern.patternDuration() <= 0) {
            return pattern.spawnCount();
        }
        double interval = Math.max(pattern.spawnInterval(), MIN_SPAWN_INTERVAL);
        return ((int) (pattern.patternDuration() / interval) + 1) * pattern.spawnCount();
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
}
