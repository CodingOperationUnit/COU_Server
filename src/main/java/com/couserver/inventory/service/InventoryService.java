package com.couserver.inventory.service;


import com.couserver.account.entity.Currency;
import com.couserver.account.repository.CurrencyRepository;
import com.couserver.common.exception.BusinessException;
import com.couserver.inventory.dto.*;
import com.couserver.inventory.entity.EquipSlotType;
import com.couserver.inventory.entity.Equipment;
import com.couserver.inventory.exception.InventoryErrorCode;
import com.couserver.inventory.repository.EquipmentRepository;
import com.couserver.staticdata.service.StaticDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final EquipmentRepository equipmentRepository;
    private final CurrencyRepository currencyRepository;
    private final StaticDataService staticDataService;
    private static final int SYNTHESIS_MATERIAL_COUNT = 2;   // 합성 재료 개수
    private static final int MAX_LEVEL = 10;
    private static final int LEVEL_UP_BASE_COST = 1000;

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(Long playerId) {
        Currency currency = currencyRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "재화 데이터가 없습니다."));

        List<EquipmentResponse> items = equipmentRepository
                .findByPlayerIdOrderByIdAsc(playerId)
                .stream()
                .map(e -> new EquipmentResponse(
                        e.getId(),
                        e.getItemId(),
                        e.getLevel(),
                        e.getGrade().toClientName(),
                        e.isEquipped()))
                .toList();

        return new InventoryResponse(currency.getCurrencyGold(), currency.getCurrencyGem(), items);
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
                e.getItemId(),
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

        // 대상 장비의 슬롯 (정적 데이터에서 조회)
        EquipSlotType slot = staticDataService.getItems().get(target.getItemId()).slotType();

        // 장착 중인 장비들 중 같은 슬롯인 것
        Equipment previous = equipmentRepository.findByPlayerIdAndEquippedTrue(playerId).stream()
                .filter(e -> staticDataService.getItems().get(e.getItemId()).slotType() == slot)
                .findFirst()
                .orElse(null);

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

    // 단일 레벨업
    @Transactional
    public LevelUpResponse levelUp(Long playerId, Long inventoryId) {
        Equipment target = getOwned(playerId, inventoryId);

        if (target.getLevel() >= MAX_LEVEL) {
            throw new BusinessException(InventoryErrorCode.MAX_LEVEL_REACHED);
        }

        Currency currency = getCurrency(playerId);
        int cost = levelUpCost(target.getLevel());
        if (currency.getCurrencyGold() < cost) {
            throw new BusinessException(InventoryErrorCode.NOT_ENOUGH_GOLD);
        }

        currency.spendGold(cost);    // 골드 차감과 레벨업이 한 트랜잭션: 중간에 실패하면 전부 롤백
        target.levelUp();

        return new LevelUpResponse(target.getId(), target.getLevel(), cost, currency.getCurrencyGold());
    }

    // 일괄 레벨업
    @Transactional
    public LevelUpBatchResponse levelUpBatch(Long playerId, Long inventoryId) {
        Equipment target = getOwned(playerId, inventoryId);

        if (target.getLevel() >= MAX_LEVEL) {
            throw new BusinessException(InventoryErrorCode.MAX_LEVEL_REACHED);
        }

        Currency currency = getCurrency(playerId);
        int spentGold = 0;
        int levelsGained = 0;

        while (target.getLevel() < MAX_LEVEL) {
            int cost = levelUpCost(target.getLevel());
            if (currency.getCurrencyGold() < cost) {
                break;                                   // 골드 부족
            }
            currency.spendGold(cost);
            target.levelUp();
            spentGold += cost;
            levelsGained++;
        }

        if (levelsGained == 0) {                         // 한 레벨도 못 올림
            throw new BusinessException(InventoryErrorCode.NOT_ENOUGH_GOLD);
        }

        return new LevelUpBatchResponse(
                target.getId(), target.getLevel(), levelsGained, spentGold, currency.getCurrencyGold());
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
                target.getId(), target.getItemId(), target.getLevel(),
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
                target.getId(), target.getItemId(), target.getLevel(),
                target.getGrade().toClientName(), target.isEquipped(),
                tiersGained, allConsumed);
    }


    // 재료 조회
    private List<Equipment> findMaterials(Long playerId, Equipment target) {
        return equipmentRepository
                .findTop2ByPlayerIdAndItemIdAndGradeAndEquippedFalseAndIdNotOrderByIdAsc(
                        playerId, target.getItemId(), target.getGrade(), target.getId());
    }

    // 1단계 합성 실행
    private List<Long> synthesizeOnce(Equipment target, List<Equipment> materials) {
        List<Long> consumedIds = materials.stream().map(Equipment::getId).toList();
        equipmentRepository.deleteAll(materials);
        target.upgradeGrade();
        return consumedIds;
    }

    // 레벨업 비용 계산
    private int levelUpCost(int level) {
        return LEVEL_UP_BASE_COST * level;
    }

    // 재화 가져오기
    private Currency getCurrency(Long playerId) {
        return currencyRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "재화 데이터가 없습니다."));
    }
}
