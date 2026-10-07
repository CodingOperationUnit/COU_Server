package com.couserver.inventory.dto;

public record LevelUpResponse(
        Long inventoryId,
        int inventoryItemLevel,   // 레벨업 후 레벨
        int spentGold,            // 이번에 차감된 골드
        int currencyGold) {}      // 차감 후 보유 골드