package com.couserver.battle.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

// 전투 입장 기록. 결과 요청을 식별하고 같은 전투로 두 번 지급하지 않게 한다
@Getter
@Entity
@Table(name = "battle_session", indexes = @Index(name = "idx_battle_session_player", columnList = "playerId"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BattleSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long battleId;

    @Column(nullable = false)
    private Long playerId;

    @Column(nullable = false)
    private int stageId;

    @Column(nullable = false, updatable = false)
    private Instant enteredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BattleStatus status;

    public BattleSession(Long playerId, int stageId, Instant enteredAt) {
        this.playerId = playerId;
        this.stageId = stageId;
        this.enteredAt = enteredAt;
        this.status = BattleStatus.IN_PROGRESS;
    }

    public void complete() {
        this.status = BattleStatus.COMPLETED;
    }
}
