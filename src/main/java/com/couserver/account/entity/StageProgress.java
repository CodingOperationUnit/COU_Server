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
}