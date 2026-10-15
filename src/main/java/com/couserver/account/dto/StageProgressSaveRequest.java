package com.couserver.account.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;

@Getter
public class StageProgressSaveRequest {
    @Min(1)
    private int currentStageId;

    // 최고 클리어 스테이지는 전투 결과로만 바뀐다
    @Deprecated
    private Integer maxClearedStageId;
}