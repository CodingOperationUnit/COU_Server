package com.couserver.battle.repository;

import com.couserver.battle.entity.BattleRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleRecordRepository extends JpaRepository<BattleRecord, Long> {
}