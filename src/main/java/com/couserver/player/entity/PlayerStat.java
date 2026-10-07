package com.couserver.player.entity;

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

    @Column(nullable = false)
    private int playerStatPotionRecoveryLevel;

    public PlayerStat(Long playerId) {
        this.playerId = playerId;
    }

    // 진화 구현 후 : 진화 레벨 별 포션 회복량을 추가해야함.
    public void update(int attackLevel, int hpLevel, int defenseLevel) {
        this.playerStatAttackLevel = attackLevel;
        this.playerStatHpLevel = hpLevel;
        this.playerStatDefenseLevel = defenseLevel;
    }

    public int getEvolutionStep(){
        return playerStatAttackLevel + playerStatHpLevel + playerStatDefenseLevel + playerStatPotionRecoveryLevel;
    }

    public PlayerStatType nextUpgradeType(){
        return PlayerStatType.ofStep(getEvolutionStep());
    }

    public PlayerStatType upgradeNext(){
        PlayerStatType type = nextUpgradeType();
        switch (type){
            case ATTACK -> playerStatAttackLevel++;
            case HP -> playerStatHpLevel++;
            case DEFENSE -> playerStatDefenseLevel++;
            case POTION_RECOVERY -> playerStatPotionRecoveryLevel++;
        }

        return type;
    }
}