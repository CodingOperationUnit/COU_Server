package com.couserver.ranking.service;

import com.couserver.account.entity.PlayerProfile;
import com.couserver.account.entity.StageProgress;
import com.couserver.account.repository.PlayerProfileRepository;
import com.couserver.account.repository.StageProgressRepository;
import com.couserver.battle.entity.StageRecord;
import com.couserver.battle.repository.StageRecordRepository;
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

    // MySQL의 기록으로 Redis 랭킹을 처음부터 다시 만든다
    @Transactional(readOnly = true)
    public void rebuildAll() {
        redisRepository.clearAll();

        int levelCount = 0;
        for (PlayerProfile p : playerProfileRepository.findAll()) {
            redisRepository.put(RankingRedisRepository.ACCOUNT_LEVEL_KEY, p.getPlayerId(),
                    RankingRedisRepository.accountLevelValue(p.getAccountLevel(), p.getAccountExp()));
            levelCount++;
        }

        int stageCount = 0;
        for (StageProgress s : stageProgressRepository.findAll()) {
            if (s.getMaxClearedStageId() == null) continue;   // 클리어 이력 없으면 제외
            redisRepository.put(RankingRedisRepository.MAX_STAGE_KEY, s.getPlayerId(), s.getMaxClearedStageId());
            stageCount++;
        }

        int survivalCount = 0;
        for (StageRecord r : stageRecordRepository.findAll()) {
            if (r.getBestSurvivalSeconds() <= 0) continue;    // 기록 없으면 제외
            redisRepository.put(RankingRedisRepository.stageSurvivalKey(r.getStageId()), r.getPlayerId(),
                    r.getBestSurvivalSeconds());
            survivalCount++;
        }

        log.info("랭킹 Redis 백필 완료: 계정레벨 {}, 최고스테이지 {}, 생존시간 {}", levelCount, stageCount, survivalCount);
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
            if (r.getBestSurvivalSeconds() > 0) {
                redisRepository.put(RankingRedisRepository.stageSurvivalKey(r.getStageId()), playerId,
                        r.getBestSurvivalSeconds());
            }
        }
    }
}