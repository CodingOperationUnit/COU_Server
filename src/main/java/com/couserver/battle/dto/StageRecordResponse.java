package com.couserver.battle.dto;

import com.couserver.battle.entity.StageRecord;

public record StageRecordResponse(
        int stageId,
        int bestClearSeconds) {          // 0이면 기록 없음

    public StageRecordResponse(StageRecord record) {
        this(record.getStageId(), record.getBestClearSeconds());
    }
}
