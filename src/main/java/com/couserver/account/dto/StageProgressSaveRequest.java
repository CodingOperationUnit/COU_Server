package com.couserver.account.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;

@Getter
public class StageProgressSaveRequest {
    @Min(1)
    private int currentStageId;

    @Min(1)
    private Integer maxClearedStageId;   // Null 허용 (null이면 @Min 검사를 건너뜀)
}