package com.couserver.evolution.dto;

import com.couserver.account.dto.CurrencyResponse;
import com.couserver.player.dto.PlayerStatResponse;
import com.couserver.player.entity.PlayerStat;
import com.couserver.player.entity.PlayerStatType;

public record EvolutionUpgradeResponse(
        PlayerStatType upgradedStatType,
        int evolutionStep,
        PlayerStatType nextStatType,
        int goldSpent,
        PlayerStatResponse playerStat,
        CurrencyResponse currency
) {
    public static EvolutionUpgradeResponse of(PlayerStatType upgraded, PlayerStatType next,
                                              PlayerStat stat, int goldSpent, CurrencyResponse currency) {
        return new EvolutionUpgradeResponse(
                upgraded,
                stat.getEvolutionStep(),
                next,
                goldSpent,
                new PlayerStatResponse(stat),
                currency
        );
    }
}
