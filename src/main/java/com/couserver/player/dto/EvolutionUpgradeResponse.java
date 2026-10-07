package com.couserver.player.dto;

import com.couserver.player.entity.PlayerStat;
import com.couserver.player.entity.PlayerStatType;

public record EvolutionUpgradeResponse(
        PlayerStatType upgradedStatType,
        int evolutionStep,
        PlayerStatType nextStatType,
        int goldSpent,
        PlayerStatResponse playerStat
) {
    public static EvolutionUpgradeResponse of(PlayerStatType upgraded, PlayerStat stat, int goldSpent){
        return new EvolutionUpgradeResponse(
                upgraded,
                stat.getEvolutionStep(),
                stat.nextUpgradeType(),
                goldSpent,
                new PlayerStatResponse(stat)
        );
    }
}
