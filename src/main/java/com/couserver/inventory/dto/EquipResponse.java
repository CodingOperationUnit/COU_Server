package com.couserver.inventory.dto;

public record EquipResponse(
        EquipmentResponse equipped,
        EquipmentResponse unequipped) {
}