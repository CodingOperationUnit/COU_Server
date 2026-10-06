package com.couserver.inventory.repository;

import com.couserver.inventory.entity.Equipment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findByPlayerId(Long playerId);
}
