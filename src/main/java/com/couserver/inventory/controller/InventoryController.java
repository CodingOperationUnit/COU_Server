package com.couserver.inventory.controller;


import com.couserver.inventory.dto.InventoryResponse;
import com.couserver.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
