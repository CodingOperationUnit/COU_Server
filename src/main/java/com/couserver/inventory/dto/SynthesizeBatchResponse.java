package com.couserver.inventory.dto;


import java.util.List;

public record SynthesizeBatchResponse(
        Long inventoryId,
        Long itemId,
        int inventoryItemLevel,
        String inventoryItemGrade,   // 최종 등급
        boolean isEquipped,
        int tiersGained,             // 1 ~ 2
        List<Long> consumedInventoryIds) {   // 소모된 재료 전부
}
