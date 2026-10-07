package com.couserver.staticdata.dto;

import java.util.List;

public record StageData(
        int stageId,
        double stageDuration,
        int waveId,
        int clearAccountExp,
        List<Integer> rewardBoxGradeWeights) {
}
