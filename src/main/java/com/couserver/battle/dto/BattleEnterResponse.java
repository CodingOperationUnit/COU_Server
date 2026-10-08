package com.couserver.battle.dto;

import com.couserver.account.dto.CurrencyResponse;

public record BattleEnterResponse(
        Long battleId,
        CurrencyResponse currency) {
}
