package com.couserver.staticdata.dto;

import java.util.List;

public record AchievementData(
        int achievementId,
        AchievementConditionType achievementConditionType,
        int targetSkillId,
        DropItemType targetDropItemType,
        List<Integer> targetValue,
        List<AchievementRewardType> rewardItemType,
        List<Integer> rewardAmount) {

    public int stepCount() {
        return targetValue.size();
    }

    // 누적값으로 달성한 단계 수 ( 0 ~ stepCount )
    public int completedStep(long progress) {
        int step = 0;
        while (step < targetValue.size() && progress >= targetValue.get(step))
            step++;

        return step;
    }
}
