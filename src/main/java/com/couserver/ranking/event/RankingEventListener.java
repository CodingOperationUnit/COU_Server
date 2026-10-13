package com.couserver.ranking.event;

import com.couserver.ranking.service.RankingRedisSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingEventListener {
    private final RankingRedisSyncService syncService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRankingChanged(RankingChangedEvent event) {
        try {
            syncService.syncPlayer(event.playerId());
        } catch (Exception e) {
            // Redis가 안되도 전투 결과 저장은 성공해야 한다. DB가 원본이라 서버 재시작 시 복구된다
            log.warn("랭킹 Redis 갱신 실패 playerId={}", event.playerId(), e);
        }

        try {
            syncService.syncCombatPower(event.playerId());
        } catch (Exception e) {
            log.warn("전투력 랭킹 Redis 갱신 실패 playerId={}", event.playerId(), e);
        }
    }
}