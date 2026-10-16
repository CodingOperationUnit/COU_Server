package com.couserver.battle.event;

import com.couserver.battle.entity.BattleRecord;
import com.couserver.battle.repository.BattleRecordRepository;
import com.couserver.battle.service.MinDamageEstimator;
import com.couserver.player.service.PlayerFinalStatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

// 전투 결과가 커밋된 직후에 통계 한 줄을 저장한다.
// 통계 저장이 실패해도 전투 결과와 보상에는 영향이 없고, 전투 결과가 롤백되면 기록도 남지 않는다
@Slf4j
@Component
public class BattleRecordListener {
    private static final int UNKNOWN = -1;   // 계산하지 못한 값

    private final BattleRecordRepository repository;
    private final PlayerFinalStatService playerFinalStatService;
    private final MinDamageEstimator minDamageEstimator;
    private final TransactionTemplate isolated;   // 매번 새 트랜잭션 (커밋이 끝난 전투 결과 트랜잭션과 섞이지 않게)

    public BattleRecordListener(BattleRecordRepository repository,
                                PlayerFinalStatService playerFinalStatService,
                                MinDamageEstimator minDamageEstimator,
                                PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.playerFinalStatService = playerFinalStatService;
        this.minDamageEstimator = minDamageEstimator;
        this.isolated = new TransactionTemplate(transactionManager);
        this.isolated.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBattleRecorded(BattleRecordedEvent event) {
        try {
            BattleRecord record = toRecord(event);
            isolated.executeWithoutResult(status -> repository.save(record));
        } catch (Exception e) {
            log.warn("전투 통계 기록 실패 battleId={}", event.battleId(), e);
        }
    }

    private BattleRecord toRecord(BattleRecordedEvent event) {
        int combatPower = UNKNOWN;
        try {
            Integer power = isolated.execute(status ->
                    playerFinalStatService.getFinalStat(event.playerId()).combatPower());
            combatPower = power == null ? UNKNOWN : power;
        } catch (Exception e) {
            log.warn("전투 통계의 전투력 계산 실패 battleId={}", event.battleId(), e);
        }

        long minDamage = UNKNOWN;
        try {
            minDamage = minDamageEstimator.estimate(event.stageId(), event.seconds(), event.kills());
        } catch (Exception e) {
            log.warn("전투 통계의 최소 피해 계산 실패 battleId={}", event.battleId(), e);
        }

        return new BattleRecord(
                event.playerId(), event.battleId(), event.stageId(),
                event.victoryReported(), event.victoryAccepted(),
                event.elapsedSeconds(), event.seconds(),
                event.kills(), event.killsLimit(),
                event.gold(), event.goldLimit(),
                event.rewardBoxes(), event.rewardBoxesLimit(),
                combatPower, minDamage, event.occurredAt());
    }
}