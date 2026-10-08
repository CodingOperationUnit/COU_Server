package com.couserver.staticdata.dto;

import com.couserver.inventory.entity.ItemGrade;

public record ShopProductData(
        int productId,
        ProductType productType,
        int priceGem,
        int rewardAmount,
        ItemGrade minGrade,
        ItemGrade maxGrade,
        boolean active) {
}
