package com.couserver.inventory.dto;


import java.util.List;

public record SynthesizeResponse(
        Long inventoryId,
        Long itemId,
        int inventoryItemLevel,
        String inventoryItemGrade,
        boolean isEquipped,
        List<Long> consumedInventoryIds) {
}
