package com.couserver.inventory.dto;

public record LevelUpBatchResponse(
        Long inventoryId,
        int inventoryItemLevel,   // 최종 레벨
        int levelsGained,         // 올라간 레벨 수
        int spentGold,            // 총 차감 골드
        int currencyGold) {}
