package com.couserver.inventory.dto;


import java.util.List;

public record InventoryResponse(
        int currencyGold,
        int currencyGem,
        List<EquipmentResponse> items) {
}
