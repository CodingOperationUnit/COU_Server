package com.couserver.inventory.service;


import com.couserver.inventory.dto.EquipResponse;
import com.couserver.inventory.dto.EquipmentResponse;
import com.couserver.inventory.dto.InventoryResponse;
import com.couserver.inventory.entity.Equipment;
import com.couserver.inventory.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    // 장비 소유 확인
    private Equipment getOwned(Long playerId, Long inventoryId) {
        Equipment equipment = equipmentRepository.findById(inventoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "INVENTORY_NOT_FOUND"));
        if (!equipment.getPlayerId().equals(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "NOT_OWNED_INVENTORY");
        }
        return equipment;
    }

    // 장비 Response 얻어오기
    private EquipmentResponse toResponse(Equipment e) {
        return new EquipmentResponse(
                e.getId(),
                e.getItem().getId(),
                e.getLevel(),
                e.getGrade().toClientName(),
                e.isEquipped());
    }

    // 장착
    @Transactional
    public EquipResponse equip(Long playerId, Long inventoryId) {
        Equipment target = getOwned(playerId, inventoryId);

        if (target.isEquipped()) {                       // 이미 장착 중이면 변화 없음
            return new EquipResponse(toResponse(target), null);
        }

        Equipment previous = equipmentRepository
                .findByPlayerIdAndItemSlotTypeAndEquippedTrue(playerId, target.getItem().getSlotType())
                .orElse(null);                           // 같은 슬롯에 장착 중인 장비

        if (previous != null) {
            previous.unequip();
        }
        target.equip();

        return new EquipResponse(toResponse(target), previous == null ? null : toResponse(previous));
    }

    // 해제
    @Transactional
    public EquipmentResponse unequip(Long playerId, Long inventoryId) {
        Equipment target = getOwned(playerId, inventoryId);

        if (!target.isEquipped()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NOT_EQUIPPED");
        }
        target.unequip();
        return toResponse(target);
    }


}
