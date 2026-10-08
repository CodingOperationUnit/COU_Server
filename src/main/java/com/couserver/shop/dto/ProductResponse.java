package com.couserver.shop.dto;

public record ProductResponse(
        int productId,
        String productType,
        int priceGem,
        int rewardAmount,
        String minGrade,
        String maxGrade) {
}