package com.couserver.battle.dto;

import com.couserver.battle.entity.StageRecord;

public record StageRecordResponse(
        int stageId,
        int bestSurvivalSeconds) {

    public StageRecordResponse(StageRecord record) {
        this(record.getStageId(), record.getBestSurvivalSeconds());
    }
}
