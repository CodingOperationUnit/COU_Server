package com.couserver.account.dto;

import com.couserver.account.entity.Currency;
import lombok.Getter;

import java.time.Instant;

@Getter
public class CurrencyResponse {
    private final Long playerId;
    private final int currencyGold;
    private final int currencyGem;
    private final int currencyEnergy;
    private final Instant currencyEnergyUpdatedAt;

    public CurrencyResponse(Currency currency) {
        this.playerId = currency.getPlayerId();
        this.currencyGold = currency.getCurrencyGold();
        this.currencyGem = currency.getCurrencyGem();
        this.currencyEnergy = currency.getCurrencyEnergy();
        this.currencyEnergyUpdatedAt = currency.getCurrencyEnergyUpdatedAt();
    }
}