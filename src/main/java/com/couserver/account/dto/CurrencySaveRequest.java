package com.couserver.account.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;

import java.time.Instant;

@Getter
public class CurrencySaveRequest {
    @Min(0)
    private int currencyGold;

    @Min(0)
    private int currencyGem;

    @Min(0)
    private int currencyEnergy;

    private Instant currencyEnergyUpdatedAt;   // Null 허용
}