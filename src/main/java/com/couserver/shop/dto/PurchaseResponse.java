package com.couserver.shop.dto;

import com.couserver.inventory.dto.EquipmentResponse;

import java.util.List;

public record PurchaseResponse(
        int productId,
        int spentGem,
        int currencyGold,
        int currencyGem,
        List<EquipmentResponse> rewardedItems) {
}