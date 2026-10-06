package com.couserver.inventory.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public record ItemData(
        long itemId,
        String itemName,
        String description,
        String slotType,
        String grade,
        int hpBonus,
        int attackBonus,
        int moveSpeedBonus) {
}

