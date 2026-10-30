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
    private int bestClearSeconds;      // 가장 짧은 클리어 시간(초). 0이면 기록 없음

    public StageRecord(Long playerId, int stageId) {
        this.playerId = playerId;
        this.stageId = stageId;
    }

    // 승리했을 때만 부른다. 기록이 없거나 더 빨리 클리어했으면 갱신한다
    public void recordClear(int seconds) {
        if (bestClearSeconds == 0 || seconds < bestClearSeconds) {
            this.bestClearSeconds = seconds;
        }
    }
}
