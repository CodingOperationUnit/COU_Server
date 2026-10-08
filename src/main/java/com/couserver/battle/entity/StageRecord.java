package com.couserver.battle.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 플레이어의 스테이지별 기록
@Getter
@Entity
@Table(name = "stage_record", uniqueConstraints = @UniqueConstraint(columnNames = {"playerId", "stageId"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StageRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stageRecordId;

    @Column(nullable = false)
    private Long playerId;

    @Column(nullable = false)
    private int stageId;

    @Column(nullable = false)
    private int bestSurvivalSeconds;

    public StageRecord(Long playerId, int stageId) {
        this.playerId = playerId;
        this.stageId = stageId;
    }

    // 승패와 관계없이 더 오래 버텼으면 갱신한다
    public void recordSurvival(int seconds) {
        if (seconds > bestSurvivalSeconds) {
            this.bestSurvivalSeconds = seconds;
        }
    }
}
