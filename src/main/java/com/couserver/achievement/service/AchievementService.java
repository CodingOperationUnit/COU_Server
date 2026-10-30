package com.couserver.achievement.service;

import com.couserver.account.dto.CurrencyResponse;
import com.couserver.account.entity.Currency;
import com.couserver.account.repository.CurrencyRepository;
import com.couserver.achievement.dto.AchievementClaimResponse;
import com.couserver.achievement.dto.BattleAchievementProgress;
import com.couserver.achievement.dto.PlayerAchievementResponse;
import com.couserver.achievement.entity.PlayerAchievement;
import com.couserver.achievement.exception.AchievementErrorCode;
import com.couserver.achievement.repository.PlayerAchievementRepository;
import com.couserver.achievement.dto.AchievementClaimAllResponse;
import com.couserver.common.exception.BusinessException;
import com.couserver.staticdata.dto.AchievementData;
import com.couserver.staticdata.service.StaticDataService;
import com.couserver.ledger.entity.LedgerReason;
import com.couserver.ledger.service.CurrencyLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementService {
    private final PlayerAchievementRepository playerAchievementRepository;
    private final CurrencyRepository currencyRepository; // 계정 담당 (잠금 조회/재화 지급에 사용)
    private final StaticDataService staticDataService;
    private final CurrencyLedgerService currencyLedger;

    // 정적 데이터의 모든 업적을 업적 ID 순으로 내려준다. 기록이 없는 업적은 진행값 0으로 채운다
    @Transactional(readOnly = true)
    public List<PlayerAchievementResponse> getAchievements(Long playerId) {
        Map<Integer, PlayerAchievement> saved = playerAchievementRepository.findByPlayerId(playerId).stream()
                .collect(Collectors.toMap(PlayerAchievement::getAchievementId, Function.identity()));

        return staticDataService.getAchievements().values().stream()
                .sorted(Comparator.comparingInt(AchievementData::achievementId))
                .map(data -> new PlayerAchievementResponse(data, saved.get(data.achievementId())))
                .toList();
    }

    // 다음 단계 보상 1개를 지급한다
    @Transactional
    public AchievementClaimResponse claim(Long playerId, int achievementId) {
        AchievementData data = staticDataService.getAchievements().get(achievementId);
        if (data == null) {
            throw new BusinessException(AchievementErrorCode.ACHIEVEMENT_NOT_FOUND);
        }

        // 재화 행을 먼저 잠근다. 같은 보상을 동시에 두 번 요청해도 차례대로 처리되어 중복 지급되지 않는다
        Currency currency = currencyRepository.findForUpdate(playerId)
                .orElseThrow(() -> new BusinessException(AchievementErrorCode.CURRENCY_NOT_FOUND));

        // 기록이 없으면 진행값이 0이라 받을 보상이 없다
        PlayerAchievement achievement = playerAchievementRepository
                .findByPlayerIdAndAchievementId(playerId, achievementId)
                .orElseThrow(() -> new BusinessException(AchievementErrorCode.NOTHING_TO_CLAIM));

        int step = achievement.getPlayerAchievementClaimedStep(); // 이번에 받을 단계 (0부터)
        if (step >= data.completedStep(achievement.getPlayerAchievementProgress())) {
            throw new BusinessException(AchievementErrorCode.NOTHING_TO_CLAIM);
        }

        int amount = data.rewardAmount().get(step);
        switch (data.rewardItemType().get(step)) {
            case GOLD -> currencyLedger.addGold(currency, amount, LedgerReason.ACHIEVEMENT_REWARD, (long) achievementId);
            case GEM -> currencyLedger.addGem(currency, amount, LedgerReason.ACHIEVEMENT_REWARD, (long) achievementId);
        }
        achievement.claimNextStep(); // 변경 감지로 자동 UPDATE

        return new AchievementClaimResponse(
                new CurrencyResponse(currency),
                new PlayerAchievementResponse(data, achievement));
    }

    // 전투 결과 트랜잭션 안에서 호출된다. 조건이 맞는 업적에 진행값을 더하고, 기록이 없으면 새로 만든다
    @Transactional
    public void addBattleProgress(Long playerId, BattleAchievementProgress battle) {
        Map<Integer, PlayerAchievement> saved = playerAchievementRepository.findByPlayerId(playerId).stream()
                .collect(Collectors.toMap(PlayerAchievement::getAchievementId, Function.identity()));

        List<PlayerAchievement> created = new ArrayList<>();
        for (AchievementData data : staticDataService.getAchievements().values()) {
            long amount = progressOf(data, battle);
            if (amount <= 0) {
                continue;
            }

            PlayerAchievement achievement = saved.get(data.achievementId());
            if (achievement == null) {
                achievement = new PlayerAchievement(playerId, data.achievementId());
                created.add(achievement);
            }
            achievement.addProgress(amount); // 기존 행은 변경 감지로 UPDATE
        }
        playerAchievementRepository.saveAll(created);
    }

    // 받을 수 있는 업적 보상을 모두 지급한다.
    // - 업적마다 대기 중인 단계(claimedStep → completedStep)를 전부 받는다
    // - 원장은 업적별·재화 종류별로 합산해서 한 줄씩 기록한다 (refId = 업적 ID)
    // - 받을 것이 하나도 없으면 NOTHING_TO_CLAIM
    @Transactional
    public AchievementClaimAllResponse claimAll(Long playerId) {
        // 재화 행을 먼저 잠근다. 단건 수령과 같은 순서라, 둘이 동시에 와도 차례대로 처리되어 중복 지급되지 않는다
        Currency currency = currencyRepository.findForUpdate(playerId)
                .orElseThrow(() -> new BusinessException(AchievementErrorCode.CURRENCY_NOT_FOUND));

        // 업적 ID 순으로 처리해서 원장 기록 순서와 응답 순서를 매번 같게 한다
        List<PlayerAchievement> records = playerAchievementRepository.findByPlayerId(playerId).stream()
                .sorted(Comparator.comparingInt(PlayerAchievement::getAchievementId))
                .toList();
        Map<Integer, AchievementData> achievementData = staticDataService.getAchievements();

        List<PlayerAchievementResponse> claimed = new ArrayList<>();
        long totalGold = 0;
        long totalGem = 0;
        int claimedStepCount = 0;

        for (PlayerAchievement record : records) {
            AchievementData data = achievementData.get(record.getAchievementId());
            if (data == null) {
                continue;   // 정적 데이터에서 빠진 업적의 기록은 건너뛴다
            }

            int fromStep = record.getPlayerAchievementClaimedStep();             // 이번에 받을 첫 단계 (0부터)
            int toStep = data.completedStep(record.getPlayerAchievementProgress()); // 여기 직전 단계까지 받는다
            if (toStep <= fromStep) {
                continue;   // 받을 단계 없음
            }

            long gold = 0;
            long gem = 0;
            for (int step = fromStep; step < toStep; step++) {
                int amount = data.rewardAmount().get(step);
                switch (data.rewardItemType().get(step)) {
                    case GOLD -> gold += amount;
                    case GEM -> gem += amount;
                }
            }

            // 한 업적의 합계가 int를 넘는 것은 데이터 이상이라, 예외로 전체를 롤백한다 (공통 처리기가 500으로 응답)
            // 보유량 + 지급량이 int를 넘는 경우는 Currency가 최대값으로 맞춘다 (단건 수령과 같은 동작)
            long refId = record.getAchievementId();
            if (gold > 0) {
                currencyLedger.addGold(currency, Math.toIntExact(gold), LedgerReason.ACHIEVEMENT_REWARD, refId);
            }
            if (gem > 0) {
                currencyLedger.addGem(currency, Math.toIntExact(gem), LedgerReason.ACHIEVEMENT_REWARD, refId);
            }
            record.claimUpTo(toStep);   // 변경 감지로 자동 UPDATE

            claimed.add(new PlayerAchievementResponse(data, record));
            totalGold += gold;
            totalGem += gem;
            claimedStepCount += toStep - fromStep;
        }

        if (claimed.isEmpty()) {
            throw new BusinessException(AchievementErrorCode.NOTHING_TO_CLAIM);
        }

        return new AchievementClaimAllResponse(
                new CurrencyResponse(currency), claimed, totalGold, totalGem, claimedStepCount);
    }

    private static long progressOf(AchievementData data, BattleAchievementProgress battle) {
        return switch (data.achievementConditionType()) {
            case KILLBYSKILL -> battle.getSkillKills().getOrDefault(data.targetSkillId(), 0);
            case PICKUPDROPITEM -> battle.getPickups().getOrDefault(data.targetDropItemType(), 0);
            case UPGRADESKILL -> data.targetSkillId() == 0
                    ? battle.totalSkillUpgrades()
                    : battle.getSkillUpgrades().getOrDefault(data.targetSkillId(), 0);
            case GAINBATTLEEXP -> battle.getBattleExp();
            case GAINBATTLEGOLD -> battle.getBattleGold();
            case CLEARSTAGE -> battle.getStageClears();
            case PLAYLUCKTRAIN -> battle.getLuckTrains();
        };
    }
}
