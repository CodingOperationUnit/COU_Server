package com.couserver.staticdata.dto;

import java.util.List;

public record StageData(
        int stageId,
        List<Integer> layerBugCounts,  // 레이어 1~5의 실제버그 수
        int referenceClearSeconds,     // 기준 클리어 시간(초). 순찰 보상과 승리 시간 경험치에 쓴다
        int clearAccountExp,
        List<Integer> rewardBoxGradeWeights) {
}
