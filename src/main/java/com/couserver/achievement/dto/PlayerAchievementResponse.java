package com.couserver.achievement.dto;

import com.couserver.achievement.entity.PlayerAchievement;
import com.couserver.staticdata.dto.AchievementData;
import lombok.Getter;

// 업적 하나의 진행 상태. completedStep은 진행값과 정적 데이터의 targetValue로 서버가 계산한다.
@Getter
public class PlayerAchievementResponse {
    private final int achievementId;
    private final long progress;
    private final int completedStep;
    private final int claimedStep;

    // saved가 null이면 아직 기록이 없는 업적이라 진행값 0, 수령 단계를 0으로 채운다.
    public PlayerAchievementResponse(AchievementData data, PlayerAchievement saved) {
        this.achievementId = data.achievementId();
        this.progress = saved == null ? 0 : saved.getPlayerAchievementProgress();
        this.completedStep = data.completedStep(this.progress);
        this.claimedStep = saved == null ? 0 : saved.getPlayerAchievementClaimedStep();
    }
}
