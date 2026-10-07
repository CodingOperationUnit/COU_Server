package com.couserver.inventory.service;


import com.couserver.common.exception.BusinessException;
import com.couserver.inventory.dto.EquipResponse;
import com.couserver.inventory.dto.EquipmentResponse;
import com.couserver.inventory.dto.InventoryResponse;
import com.couserver.inventory.dto.SynthesizeResponse;
import com.couserver.inventory.entity.Equipment;
import com.couserver.inventory.exception.InventoryErrorCode;
import com.couserver.inventory.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final EquipmentRepository equipmentRepository;

    private static final int SYNTHESIS_MATERIAL_COUNT = 2;   // 합성 재료 개수

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
                .orElseThrow(() -> new BusinessException(InventoryErrorCode.INVENTORY_NOT_FOUND));
        if (!equipment.getPlayerId().equals(playerId)) {
            throw new BusinessException(InventoryErrorCode.NOT_OWNED_INVENTORY);
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
            throw new BusinessException(InventoryErrorCode.NOT_EQUIPPED);
        }
        target.unequip();
        return toResponse(target);
    }


    // 장비 합성 로직
    @Transactional
    public SynthesizeResponse synthesize(Long playerId, Long inventoryId) {
        Equipment target = getOwned(playerId, inventoryId);

        // 최고등급이면 합성안됨
        if (!target.getGrade().canSynthesize()) {
            throw new BusinessException(InventoryErrorCode.CANNOT_SYNTHESIZE_MAX_GRADE);
        }

        // 장비 재료 찾기
        List<Equipment> materials = equipmentRepository
                .findTop2ByPlayerIdAndItem_IdAndGradeAndEquippedFalseAndIdNotOrderByIdAsc(
                        playerId, target.getItem().getId(), target.getGrade(), target.getId());
        if (materials.size() < SYNTHESIS_MATERIAL_COUNT) {
            throw new BusinessException(InventoryErrorCode.NOT_ENOUGH_MATERIAL);
        }

        // 3) 합성가능하면 재료삭제(실패 시 전부 롤백)
        List<Long> consumedIds = materials.stream().map(Equipment::getId).toList();
        equipmentRepository.deleteAll(materials);
        target.upgradeGrade();

        return new SynthesizeResponse(
                target.getId(),
                target.getItem().getId(),
                target.getLevel(),
                target.getGrade().toClientName(),  // 등급을 올린 뒤 합성
                target.isEquipped(),
                consumedIds);
    }

}
