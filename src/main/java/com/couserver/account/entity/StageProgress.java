package com.couserver.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "stage_progress")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StageProgress {
    @Id
    private Long playerId;

    @Column(nullable = false)
    private int currentStageId;

    private Integer maxClearedStageId;   // 클리어 이력 없으면 Null

    public StageProgress(Long playerId, int firstStageId) {
        this.playerId = playerId;
        this.currentStageId = firstStageId;
    }

    public void update(int currentStageId, Integer maxClearedStageId) {
        this.currentStageId = currentStageId;
        this.maxClearedStageId = maxClearedStageId;
    }

    // 전투한 스테이지를 현재 스테이지로 두고, 승리했고 더 높으면 최고 클리어 스테이지를 갱신한다
    public void recordBattle(int stageId, boolean victory) {
        this.currentStageId = stageId;
        if (victory && (maxClearedStageId == null || stageId > maxClearedStageId)) {
            this.maxClearedStageId = stageId;
        }
    }
}