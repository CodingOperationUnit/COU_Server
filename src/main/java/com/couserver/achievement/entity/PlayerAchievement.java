package com.couserver.achievement.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "player_achievement",
            uniqueConstraints = @UniqueConstraint(columnNames = {"playerId", "achievementId"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerAchievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long playerAchievementId;

    @Column(nullable = false)
    private Long playerId;

    @Column(nullable = false)
    private int achievementId;

    @Column(nullable = false)
    private long playerAchievementProgress;

    @Column(nullable = false)
    private int playerAchievementClaimedStep;

    public PlayerAchievement(Long playerId, int achievementId) {
        this.playerId = playerId;
        this.achievementId = achievementId;
    }

    // 전투 결과에서 누적값을 더한다
    public void addProgress(long amount) {
        this.playerAchievementProgress += amount;
    }

    // 다음 단계 보상을 받았음을 기록한다. 받을 수 있는지는 서비스가 먼저 확인한다.
    public void claimNextStep() {
        this.playerAchievementClaimedStep++;
    }

    // 지정한 단계까지 보상을 모두 받았음을 기록한다 (모두 받기용).
    // 받을 수 있는지는 서비스가 먼저 확인하고, 수령 단계가 줄어드는 호출은 막는다.
    public void claimUpTo(int step) {
        if (step < this.playerAchievementClaimedStep) {
            throw new IllegalArgumentException(
                    "수령 단계는 줄어들 수 없습니다: " + this.playerAchievementClaimedStep + " → " + step);
        }
        this.playerAchievementClaimedStep = step;
    }
}
