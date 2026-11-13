package com.couserver.ranking.service;

import com.couserver.account.entity.AccountType;
import com.couserver.account.entity.PlayerProfile;
import com.couserver.account.entity.StageProgress;
import com.couserver.account.repository.PlayerProfileRepository;
import com.couserver.account.repository.StageProgressRepository;
import com.couserver.battle.entity.StageRecord;
import com.couserver.battle.repository.StageRecordRepository;
import com.couserver.player.service.PlayerFinalStatService;
import com.couserver.ranking.repository.RankingRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingRedisSyncService {
    private final RankingRedisRepository redisRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final StageProgressRepository stageProgressRepository;
    private final StageRecordRepository stageRecordRepository;
    private final PlayerFinalStatService playerFinalStatService;

    // MySQL의 기록으로 Redis 랭킹을 처음부터 다시 만든다
    // 여기서는 트랜잭션을 걸지 않는다: 전투력 계산(getFinalStat)이 예외를 던지면 바깥 트랜잭션이 rollback-only로 표시되어
    // 예외를 잡아도 UnexpectedRollbackException이 나기 때문이다. 조회는 각 레포지토리 호출이 알아서 처리한다
    public void rebuildAll() {
        redisRepository.clearAll();

        int levelCount = 0;
        int combatPowerCount = 0;
        Set<Long> rankedPlayerIds = new HashSet<>();
        for (PlayerProfile p : playerProfileRepository.findAllWithAccount()) {
            if (!p.getAccount().getAccountType().isRanked()) continue;   // 테스트 계정 등은 제외
            rankedPlayerIds.add(p.getPlayerId());

            redisRepository.put(RankingRedisRepository.ACCOUNT_LEVEL_KEY, p.getPlayerId(),
                    RankingRedisRepository.accountLevelValue(p.getAccountLevel(), p.getAccountExp()));
            levelCount++;

            if (putCombatPower(p.getPlayerId())) combatPowerCount++;
        }

        int stageCount = 0;
        for (StageProgress s : stageProgressRepository.findAll()) {
            if (!rankedPlayerIds.contains(s.getPlayerId())) continue;
            if (s.getMaxClearedStageId() == null) continue;   // 클리어 이력 없으면 제외
            redisRepository.put(RankingRedisRepository.MAX_STAGE_KEY, s.getPlayerId(), s.getMaxClearedStageId());
            stageCount++;
        }

        int survivalCount = 0;
        for (StageRecord r : stageRecordRepository.findAll()) {
            if (!rankedPlayerIds.contains(r.getPlayerId())) continue;
            if (r.getBestSurvivalSeconds() <= 0) continue;    // 기록 없으면 제외
            redisRepository.put(RankingRedisRepository.stageSurvivalKey(r.getStageId()), r.getPlayerId(),
                    r.getBestSurvivalSeconds());
            survivalCount++;
        }

        log.info("랭킹 Redis 백필 완료: 계정레벨 {}, 최고스테이지 {}, 생존시간 {}, 전투력 {}",
                levelCount, stageCount, survivalCount, combatPowerCount);
    }

    // 한 플레이어의 랭킹 값을 DB 기준으로 Redis에 다시 반영한다
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void syncPlayer(Long playerId) {
        if (!isRanked(playerId)) return;

        playerProfileRepository.findById(playerId).ifPresent(p ->
                redisRepository.put(RankingRedisRepository.ACCOUNT_LEVEL_KEY, playerId,
                        RankingRedisRepository.accountLevelValue(p.getAccountLevel(), p.getAccountExp())));

        stageProgressRepository.findById(playerId).ifPresent(s -> {
            if (s.getMaxClearedStageId() != null) {
                redisRepository.put(RankingRedisRepository.MAX_STAGE_KEY, playerId, s.getMaxClearedStageId());
            }
        });

        for (StageRecord r : stageRecordRepository.findByPlayerIdOrderByStageIdAsc(playerId)) {
            if (r.getBestSurvivalSeconds() > 0) {
                redisRepository.put(RankingRedisRepository.stageSurvivalKey(r.getStageId()), playerId,
                        r.getBestSurvivalSeconds());
            }
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void syncCombatPower(Long playerId) {
        if (!isRanked(playerId)) return;

        putCombatPower(playerId);
    }

    // 랭킹에 올리는 계정인지. 플레이어가 없으면 올리지 않는다
    private boolean isRanked(Long playerId) {
        return playerProfileRepository.findAccountTypeByPlayerId(playerId)
                .map(AccountType::isRanked)
                .orElse(false);
    }

    // 전투력은 DB에 저장하지 않고 장착 장비와 진화 스탯으로 계산한 값을 Redis에 넣는다
    private boolean putCombatPower(Long playerId) {
        try {
            int combatPower = playerFinalStatService.getFinalStat(playerId).combatPower();
            redisRepository.put(RankingRedisRepository.COMBAT_POWER_KEY, playerId, combatPower);
            return true;
        } catch (Exception e) {
            log.warn("전투력 랭킹 계산 실패 playerId={}", playerId, e);
            return false;
        }
    }
}
