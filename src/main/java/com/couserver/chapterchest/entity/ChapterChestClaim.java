package com.couserver.chapterchest.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

// 플레이어가 받은 챕터 보물상자. 스테이지마다 한 번만 받을 수 있다
@Getter
@Entity
@Table(name = "chapter_chest_claim",
        uniqueConstraints = @UniqueConstraint(columnNames = {"playerId", "stageId"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChapterChestClaim {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chapterChestClaimId;

    @Column(nullable = false)
    private Long playerId;

    @Column(nullable = false)
    private int stageId;

    @Column(nullable = false)
    private Instant claimedAt;

    public ChapterChestClaim(Long playerId, int stageId, Instant claimedAt) {
        this.playerId = playerId;
        this.stageId = stageId;
        this.claimedAt = claimedAt;
    }
}
