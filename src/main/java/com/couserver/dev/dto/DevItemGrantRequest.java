package com.couserver.dev.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DevItemGrantRequest(
        @NotNull Long itemId,
        @NotNull @Min(1) @Max(20) Integer count) {
}
