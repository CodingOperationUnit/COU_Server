package com.couserver.inventory.service;


import com.couserver.inventory.dto.EquipmentResponse;
import com.couserver.inventory.dto.InventoryResponse;
import com.couserver.inventory.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final EquipmentRepository equipmentRepository;

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(Long playerId) {
        List<EquipmentResponse> items = equipmentRepository
                .findByPlayerIdOrderByIdAsc(playerId)
                .stream()
                .map(e -> new EquipmentResponse(
                        e.getId(),
                        e.getItem().getId(),
                        e.getLevel(),
                        e.getGrade().toClientName(),
                        e.isEquipped()))
                .toList();

        return new InventoryResponse(0, 0, items); // 임시 재화값
    }
}
