package com.couserver.player.dto;

import com.couserver.player.entity.PlayerStat;
import lombok.Getter;

@Getter
public class PlayerStatResponse {
    private final Long playerId;
    private final int playerStatAttackLevel;
    private final int playerStatHpLevel;
    private final int playerStatDefenseLevel;
    private final int playerStatPotionRecoveryLevel;

    public PlayerStatResponse(PlayerStat playerStat) {
        this.playerId = playerStat.getPlayerId();
        this.playerStatAttackLevel = playerStat.getPlayerStatAttackLevel();
        this.playerStatHpLevel = playerStat.getPlayerStatHpLevel();
        this.playerStatDefenseLevel = playerStat.getPlayerStatDefenseLevel();
        this.playerStatPotionRecoveryLevel = playerStat.getPlayerStatPotionRecoveryLevel();
    }
}