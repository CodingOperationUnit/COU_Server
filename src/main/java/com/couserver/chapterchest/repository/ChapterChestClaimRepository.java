package com.couserver.chapterchest.repository;

import com.couserver.chapterchest.entity.ChapterChestClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChapterChestClaimRepository extends JpaRepository<ChapterChestClaim, Long> {
    boolean existsByPlayerIdAndStageId(Long playerId, int stageId);
    List<ChapterChestClaim> findByPlayerIdOrderByStageIdAsc(Long playerId);
}
