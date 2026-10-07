package com.couserver.inventory.service;


import com.couserver.common.exception.BusinessException;
import com.couserver.inventory.dto.*;
import com.couserver.inventory.entity.Equipment;
import com.couserver.inventory.exception.InventoryErrorCode;
import com.couserver.inventory.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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


    // 장비 합성 로직(단일 합성)
    @Transactional
    public SynthesizeResponse synthesize(Long playerId, Long inventoryId) {
        Equipment target = getOwned(playerId, inventoryId);

        if (!target.getGrade().canSynthesize()) {
            throw new BusinessException(InventoryErrorCode.CANNOT_SYNTHESIZE_MAX_GRADE);
        }

        List<Equipment> materials = findMaterials(playerId, target);
        if (materials.size() < SYNTHESIS_MATERIAL_COUNT) {
            throw new BusinessException(InventoryErrorCode.NOT_ENOUGH_MATERIAL);
        }

        List<Long> consumedIds = synthesizeOnce(target, materials);

        return new SynthesizeResponse(
                target.getId(), target.getItem().getId(), target.getLevel(),
                target.getGrade().toClientName(), target.isEquipped(), consumedIds);
    }

    // 장비 합성 로직(일괄 합성)
    @Transactional
    public SynthesizeBatchResponse synthesizeBatch(Long playerId, Long inventoryId) {
        Equipment target = getOwned(playerId, inventoryId);

        // 최고 등급일 때 예외처리
        if (!target.getGrade().canSynthesize()) {
            throw new BusinessException(InventoryErrorCode.CANNOT_SYNTHESIZE_MAX_GRADE);
        }

        List<Long> allConsumed = new ArrayList<>();
        int tiersGained = 0;

        while (target.getGrade().canSynthesize()) {
            List<Equipment> materials = findMaterials(playerId, target);   // 현재 등급 기준으로 매번 재조회
            if (materials.size() < SYNTHESIS_MATERIAL_COUNT) {
                break;                                                      // 재료부족 시 강제 종료
            }
            allConsumed.addAll(synthesizeOnce(target, materials));
            tiersGained++;
        }

        if (tiersGained == 0) {                                             // 한 단계도 못 올림
            throw new BusinessException(InventoryErrorCode.NOT_ENOUGH_MATERIAL);
        }

        return new SynthesizeBatchResponse(
                target.getId(), target.getItem().getId(), target.getLevel(),
                target.getGrade().toClientName(), target.isEquipped(),
                tiersGained, allConsumed);
    }


    // 재료 조회
    private List<Equipment> findMaterials(Long playerId, Equipment target) {
        return equipmentRepository
                .findTop2ByPlayerIdAndItem_IdAndGradeAndEquippedFalseAndIdNotOrderByIdAsc(
                        playerId, target.getItem().getId(), target.getGrade(), target.getId());
    }

    // 1단계 합성 실행
    private List<Long> synthesizeOnce(Equipment target, List<Equipment> materials) {
        List<Long> consumedIds = materials.stream().map(Equipment::getId).toList();
        equipmentRepository.deleteAll(materials);
        target.upgradeGrade();
        return consumedIds;
    }

}
