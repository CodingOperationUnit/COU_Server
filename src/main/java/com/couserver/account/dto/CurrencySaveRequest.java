package com.couserver.account.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CurrencySaveRequest {
    @Min(0)
    private int currencyGold;

    @Min(0)
    private int currencyGem;

    @Min(0)
    private int currencyEnergy;

    private LocalDateTime currencyEnergyUpdatedAt;   // Null 허용
}