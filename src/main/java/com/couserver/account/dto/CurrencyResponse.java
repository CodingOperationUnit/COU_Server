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

    // 회복 계산 결과를 응답에만 담는다 (엔티티를 바꾸지 않는 조회용)
    public CurrencyResponse(Currency currency, Currency.EnergyState energy) {
        this.playerId = currency.getPlayerId();
        this.currencyGold = currency.getCurrencyGold();
        this.currencyGem = currency.getCurrencyGem();
        this.currencyEnergy = energy.energy();
        this.currencyEnergyUpdatedAt = energy.updatedAt();
    }
}