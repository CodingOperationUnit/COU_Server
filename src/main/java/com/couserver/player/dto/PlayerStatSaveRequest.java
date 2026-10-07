package com.couserver.player.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;

@Getter
public class PlayerStatSaveRequest {
    @Min(0)
    private int playerStatAttackLevel;

    @Min(0)
    private int playerStatHpLevel;

    @Min(0)
    private int playerStatDefenseLevel;
}