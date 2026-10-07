package com.couserver.account.dto;

import com.couserver.inventory.entity.Equipment;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class InventoryItemResponse {
    private final Long inventoryId;
    private final Long playerId;
    private final Long itemId;
    private final int inventoryItemLevel;
    private final LocalDateTime inventoryAcquiredAt;
    private final String inventoryItemGrade;

    public InventoryItemResponse(Equipment equipment) {
        this.inventoryId = equipment.getId();
        this.playerId = equipment.getPlayerId();
        this.itemId = equipment.getItem().getId();
        this.inventoryItemLevel = equipment.getLevel();
        this.inventoryAcquiredAt = equipment.getAcquiredAt();
        this.inventoryItemGrade = equipment.getGrade().toClientName();
    }
}
