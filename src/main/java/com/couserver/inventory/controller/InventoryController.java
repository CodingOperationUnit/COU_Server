package com.couserver.inventory.controller;


import com.couserver.account.dto.AuthAccount;
import com.couserver.inventory.dto.*;
import com.couserver.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public InventoryResponse getInventory(@AuthenticationPrincipal AuthAccount authAccount) {
        return inventoryService.getInventory(authAccount.getPlayerId());
    }

    @PostMapping("/{inventoryId}/equip")
    public EquipResponse equip(@AuthenticationPrincipal AuthAccount authAccount, @PathVariable Long inventoryId) {
        return inventoryService.equip(authAccount.getPlayerId(), inventoryId);
    }

    @PostMapping("/{inventoryId}/unequip")
    public EquipmentResponse unequip(@AuthenticationPrincipal AuthAccount authAccount, @PathVariable Long inventoryId) {
        return inventoryService.unequip(authAccount.getPlayerId(), inventoryId);
    }

    @PostMapping("/{inventoryId}/synthesize")
    public SynthesizeResponse synthesize(@AuthenticationPrincipal AuthAccount authAccount,@PathVariable Long inventoryId) {
        return inventoryService.synthesize(authAccount.getPlayerId(), inventoryId);
    }

    @PostMapping("/{inventoryId}/synthesize/batch")
    public SynthesizeBatchResponse synthesizeBatch(@AuthenticationPrincipal AuthAccount authAccount, @PathVariable Long inventoryId) {
        return inventoryService.synthesizeBatch(authAccount.getPlayerId(), inventoryId);
    }

    @PostMapping("/{inventoryId}/levelup")
    public LevelUpResponse levelUp(@AuthenticationPrincipal AuthAccount authAccount,
                                   @PathVariable Long inventoryId) {
        return inventoryService.levelUp(authAccount.getPlayerId(), inventoryId);
    }

    @PostMapping("/{inventoryId}/levelup/batch")
    public LevelUpBatchResponse levelUpBatch(@AuthenticationPrincipal AuthAccount authAccount,
                                             @PathVariable Long inventoryId) {
        return inventoryService.levelUpBatch(authAccount.getPlayerId(), inventoryId);
    }

}
