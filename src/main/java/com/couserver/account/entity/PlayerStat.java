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
@Table(name = "player_stat")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerStat {
    @Id
    private Long playerId;

    @Column(nullable = false)
    private int playerStatAttackLevel;

    @Column(nullable = false)
    private int playerStatHpLevel;

    @Column(nullable = false)
    private int playerStatDefenseLevel;

    public PlayerStat(Long playerId) {
        this.playerId = playerId;
    }

    public void update(int attackLevel, int hpLevel, int defenseLevel) {
        this.playerStatAttackLevel = attackLevel;
        this.playerStatHpLevel = hpLevel;
        this.playerStatDefenseLevel = defenseLevel;
    }
}