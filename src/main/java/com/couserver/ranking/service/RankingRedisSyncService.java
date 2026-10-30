package com.couserver.ranking.service;

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
        for (PlayerProfile p : playerProfileRepository.findAll()) {
            redisRepository.put(RankingRedisRepository.ACCOUNT_LEVEL_KEY, p.getPlayerId(),
                    RankingRedisRepository.accountLevelValue(p.getAccountLevel(), p.getAccountExp()));
            levelCount++;

            if (putCombatPower(p.getPlayerId())) combatPowerCount++;
        }

        int stageCount = 0;
        for (StageProgress s : stageProgressRepository.findAll()) {
            if (s.getMaxClearedStageId() == null) continue;   // 클리어 이력 없으면 제외
            redisRepository.put(RankingRedisRepository.MAX_STAGE_KEY, s.getPlayerId(), s.getMaxClearedStageId());
            stageCount++;
        }

        int clearCount = 0;
        for (StageRecord r : stageRecordRepository.findAll()) {
            if (r.getBestClearSeconds() <= 0) continue;    // 기록 없으면 제외
            redisRepository.put(RankingRedisRepository.stageClearKey(r.getStageId()), r.getPlayerId(),
                    RankingRedisRepository.clearTimeValue(r.getBestClearSeconds()));
            clearCount++;
        }

        log.info("랭킹 Redis 백필 완료: 계정레벨 {}, 최고스테이지 {}, 클리어시간 {}, 전투력 {}",
                levelCount, stageCount, clearCount, combatPowerCount);
    }

    // 한 플레이어의 랭킹 값을 DB 기준으로 Redis에 다시 반영한다
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void syncPlayer(Long playerId) {
        playerProfileRepository.findById(playerId).ifPresent(p ->
                redisRepository.put(RankingRedisRepository.ACCOUNT_LEVEL_KEY, playerId,
                        RankingRedisRepository.accountLevelValue(p.getAccountLevel(), p.getAccountExp())));

        stageProgressRepository.findById(playerId).ifPresent(s -> {
            if (s.getMaxClearedStageId() != null) {
                redisRepository.put(RankingRedisRepository.MAX_STAGE_KEY, playerId, s.getMaxClearedStageId());
            }
        });

        for (StageRecord r : stageRecordRepository.findByPlayerIdOrderByStageIdAsc(playerId)) {
            if (r.getBestClearSeconds() > 0) {
                redisRepository.put(RankingRedisRepository.stageClearKey(r.getStageId()), playerId,
                        RankingRedisRepository.clearTimeValue(r.getBestClearSeconds()));
            }
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void syncCombatPower(Long playerId) {
        putCombatPower(playerId);
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
