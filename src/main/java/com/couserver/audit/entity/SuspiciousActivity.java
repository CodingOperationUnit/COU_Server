package com.couserver.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

// 서버가 비정상으로 판단한 요청의 기록. 자동 제재 없이, 사람이 보고 판단하기 위한 자료다
@Getter
@Entity
@Table(name = "suspicious_activity",
        indexes = @Index(name = "idx_suspicious_activity_player", columnList = "playerId, createdAt"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SuspiciousActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SuspiciousActivityType activityType;

    @Column(nullable = false, length = 500)
    private String detail;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public SuspiciousActivity(Long playerId, SuspiciousActivityType activityType, String detail, Instant createdAt) {
        this.playerId = playerId;
        this.activityType = activityType;
        this.detail = detail;
        this.createdAt = createdAt;
    }
}