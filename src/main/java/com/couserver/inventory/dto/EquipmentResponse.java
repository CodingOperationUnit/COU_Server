package com.couserver.inventory.dto;

public record EquipmentResponse(
        Long inventoryId,
        Long itemId,
        int inventoryItemLevel,
        String inventoryItemGrade,
        boolean isEquipped) {
}