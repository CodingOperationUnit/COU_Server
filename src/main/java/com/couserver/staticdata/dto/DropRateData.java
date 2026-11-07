package com.couserver.staticdata.dto;

import com.couserver.inventory.entity.ItemGrade;

import java.math.BigDecimal;

public record DropRateData(
        String dropRateId,
        int minAccountLevel,
        int maxAccountLevel,
        BigDecimal commonRate,
        BigDecimal uncommonRate,
        BigDecimal rareRate,
        BigDecimal epicRate,
        BigDecimal legendaryRate) {

    public BigDecimal rateOf(ItemGrade grade) {
        return switch (grade) {
            case COMMON -> commonRate;
            case UNCOMMON -> uncommonRate;
            case RARE -> rareRate;
            case EPIC -> epicRate;
            case LEGENDARY -> legendaryRate;
        };
    }

    public boolean isUnlimited() {
        return maxAccountLevel == 0;
    }

    public boolean covers(int accountLevel) {
        return accountLevel >= minAccountLevel && (isUnlimited() || accountLevel <= maxAccountLevel);
    }
}