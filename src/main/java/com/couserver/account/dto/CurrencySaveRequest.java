package com.couserver.account.dto;

import lombok.Getter;

import java.time.Instant;

// 의심 활동 기록에서 비교용으로만 읽는다
@Getter
public class CurrencySaveRequest {
    private Integer currencyGold;
    private Integer currencyGem;
    private Integer currencyEnergy;
    private Instant currencyEnergyUpdatedAt;
}