package com.couserver.staticdata.dto;

import com.couserver.inventory.entity.EquipSlotType;
import com.couserver.inventory.entity.ItemGrade;

public record ItemData(
        long itemId,
        EquipSlotType slotType,
        int baseHpBonus,
        int baseAttackBonus) {
}