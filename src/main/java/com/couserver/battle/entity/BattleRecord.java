package com.couserver.battle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

// 결과가 처리된 전투마다 한 줄. 이상 계정을 SQL로 찾기 위한 통계다 (battle_session은 플레이어당 최근 한 건만 남아 쓸 수 없다).
// 값이 -1인 combatPower, minDamage는 계산하지 못했다는 뜻이다
@Getter
@Entity
@Table(name = "battle_record",
        indexes = @Index(name = "idx_battle_record_player", columnList = "playerId, createdAt"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BattleRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long playerId;

    @Column(nullable = false, unique = true)
    private Long battleId;

    @Column(nullable = false)
    private int stageId;

    @Column(nullable = false)
    private boolean victoryReported;   // 클라이언트가 보고한 승리

    @Column(nullable = false)
    private boolean victoryAccepted;   // 서버가 인정한 승리 (너무 빠른 승리는 인정하지 않는다)

    @Column(nullable = false)
    private long elapsedSeconds;       // 서버가 잰 입장 후 경과 시간

    @Column(nullable = false)
    private int seconds;               // 인정한 전투 시간

    @Column(nullable = false)
    private int kills;

    @Column(nullable = false)
    private int killsLimit;            // 그 시각까지 나온 몬스터로 계산한 상한

    @Column(nullable = false)
    private int gold;

    @Column(nullable = false)
    private int goldLimit;

    @Column(nullable = false)
    private int rewardBoxes;

    @Column(nullable = false)
    private int rewardBoxesLimit;

    @Column(nullable = false)
    private int combatPower;           // 결과 처리 시점의 전투력

    @Column(nullable = false)
    private long minDamage;            // 인정한 킬 수를 채우는 데 최소한 필요한 피해 (추정 하한)

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public BattleRecord(Long playerId, Long battleId, int stageId, boolean victoryReported, boolean victoryAccepted,
                        long elapsedSeconds, int seconds, int kills, int killsLimit, int gold, int goldLimit,
                        int rewardBoxes, int rewardBoxesLimit, int combatPower, long minDamage, Instant createdAt) {
        this.playerId = playerId;
        this.battleId = battleId;
        this.stageId = stageId;
        this.victoryReported = victoryReported;
        this.victoryAccepted = victoryAccepted;
        this.elapsedSeconds = elapsedSeconds;
        this.seconds = seconds;
        this.kills = kills;
        this.killsLimit = killsLimit;
        this.gold = gold;
        this.goldLimit = goldLimit;
        this.rewardBoxes = rewardBoxes;
        this.rewardBoxesLimit = rewardBoxesLimit;
        this.combatPower = combatPower;
        this.minDamage = minDamage;
        this.createdAt = createdAt;
    }
}