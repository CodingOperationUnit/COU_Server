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
