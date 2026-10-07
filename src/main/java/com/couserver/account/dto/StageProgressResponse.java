package com.couserver.account.dto;

import com.couserver.account.entity.StageProgress;
import lombok.Getter;

@Getter
public class StageProgressResponse {
    private final Long playerId;
    private final int currentStageId;
    private final Integer maxClearedStageId;

    public StageProgressResponse(StageProgress stageProgress) {
        this.playerId = stageProgress.getPlayerId();
        this.currentStageId = stageProgress.getCurrentStageId();
        this.maxClearedStageId = stageProgress.getMaxClearedStageId();
    }
}