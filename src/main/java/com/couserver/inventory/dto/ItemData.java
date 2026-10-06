package com.couserver.inventory.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
public record ItemData(
        long itemId,
        String itemName,
        String description,
        String iconPath,
        String slotType,
        String grade,
        int hpBonus,
        int attackBonus,
        int moveSpeedBonus) {
}

