package com.couserver.inventory.controller;


import com.couserver.inventory.dto.*;
import com.couserver.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private static final Long TEMP_PLAYER_ID = 1L; // 로그인 구현 전 임시값

    private final InventoryService inventoryService;

    @GetMapping
    public InventoryResponse getInventory() {
        return inventoryService.getInventory(TEMP_PLAYER_ID);
    }

    @PostMapping("/{inventoryId}/equip")
    public EquipResponse equip(@PathVariable Long inventoryId) {
        return inventoryService.equip(TEMP_PLAYER_ID, inventoryId);
    }

    @PostMapping("/{inventoryId}/unequip")
    public EquipmentResponse unequip(@PathVariable Long inventoryId) {
        return inventoryService.unequip(TEMP_PLAYER_ID, inventoryId);
    }

    @PostMapping("/{inventoryId}/synthesize")
    public SynthesizeResponse synthesize(@PathVariable Long inventoryId) {
        return inventoryService.synthesize(TEMP_PLAYER_ID, inventoryId);
    }

    @PostMapping("/{inventoryId}/synthesize/batch")
    public SynthesizeBatchResponse synthesizeBatch(@PathVariable Long inventoryId) {
        return inventoryService.synthesizeBatch(TEMP_PLAYER_ID, inventoryId);
    }


}
