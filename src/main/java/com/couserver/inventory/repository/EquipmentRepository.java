package com.couserver.inventory.repository;

import com.couserver.inventory.entity.EquipSlotType;
import com.couserver.inventory.entity.Equipment;
import java.util.List;
import java.util.Optional;

import com.couserver.inventory.entity.ItemGrade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findByPlayerId(Long playerId);
    List<Equipment> findByPlayerIdOrderByIdAsc(Long playerId);
    Optional<Equipment> findByPlayerIdAndItemSlotTypeAndEquippedTrue(Long playerId, EquipSlotType slotType);
    List<Equipment> findTop2ByPlayerIdAndItem_IdAndGradeAndEquippedFalseAndIdNotOrderByIdAsc(
            Long playerId, Long itemId, ItemGrade grade, Long id);
}
