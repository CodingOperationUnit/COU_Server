package com.couserver.battle.event;

import java.time.Instant;

// 전투 결과가 처리됐다는 알림. 커밋된 뒤에 BattleRecordListener가 통계를 저장한다
public record BattleRecordedEvent(
        Long playerId,
        Long battleId,
        int stageId,
        boolean victoryReported,
        boolean victoryAccepted,
        long elapsedSeconds,
        int seconds,
        int kills,
        int killsLimit,
        int gold,
        int goldLimit,
        int rewardBoxes,
        int rewardBoxesLimit,
        Instant occurredAt) {
}