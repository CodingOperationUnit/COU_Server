package com.couserver.shop.dto;


import jakarta.validation.constraints.NotNull;

public record PurchaseRequest(
        @NotNull Integer productId) {
}
