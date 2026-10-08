package com.couserver.battle.repository;

import com.couserver.battle.entity.StageRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StageRecordRepository extends JpaRepository<StageRecord, Long> {

    Optional<StageRecord> findByPlayerIdAndStageId(Long playerId, int stageId);

    List<StageRecord> findByPlayerIdOrderByStageIdAsc(Long playerId);
}
