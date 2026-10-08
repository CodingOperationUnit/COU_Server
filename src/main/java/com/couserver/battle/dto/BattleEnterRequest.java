package com.couserver.battle.dto;

import jakarta.validation.constraints.Min;

public record BattleEnterRequest(
        @Min(1) int stageId) {
}
