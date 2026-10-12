package com.couserver.ranking.repository;

import com.couserver.battle.entity.StageRecord;
import com.couserver.ranking.dto.RankingRow;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RankingRepository extends Repository<StageRecord, Long> {

    // 계정 레벨
    @Query("""
        select new com.couserver.ranking.dto.RankingRow(p.playerId, p.playerNickname, p.accountLevel, p.accountLevel)
        from PlayerProfile p
        order by p.accountLevel desc, p.accountExp desc, p.playerId asc
    """)
    List<RankingRow> findTopByAccountLevel(Pageable pageable);

    @Query("""
        select count(p) from PlayerProfile p
        where p.accountLevel > :level
           or (p.accountLevel = :level and p.accountExp > :exp)
           or (p.accountLevel = :level and p.accountExp = :exp and p.playerId < :playerId)
    """)
    long countAheadByAccountLevel(@Param("level") int level, @Param("exp") int exp, @Param("playerId") Long playerId);

    // 최고 클리어 스테이지
    @Query("""
        select new com.couserver.ranking.dto.RankingRow(p.playerId, p.playerNickname, p.accountLevel, s.maxClearedStageId)
        from StageProgress s join PlayerProfile p on p.playerId = s.playerId
        where s.maxClearedStageId is not null
        order by s.maxClearedStageId desc, s.playerId asc
    """)
    List<RankingRow> findTopByMaxStage(Pageable pageable);

    @Query("""
        select count(s) from StageProgress s
        where s.maxClearedStageId is not null
          and (s.maxClearedStageId > :stage or (s.maxClearedStageId = :stage and s.playerId < :playerId))
    """)
    long countAheadByMaxStage(@Param("stage") int stage, @Param("playerId") Long playerId);

    // 스테이지별 생존 시간
    @Query("""
        select new com.couserver.ranking.dto.RankingRow(p.playerId, p.playerNickname, p.accountLevel, r.bestSurvivalSeconds)
        from StageRecord r join PlayerProfile p on p.playerId = r.playerId
        where r.stageId = :stageId and r.bestSurvivalSeconds > 0
        order by r.bestSurvivalSeconds desc, r.playerId asc
    """)
    List<RankingRow> findTopByStageSurvival(@Param("stageId") int stageId, Pageable pageable);

    @Query("""
        select count(r) from StageRecord r
        where r.stageId = :stageId
          and (r.bestSurvivalSeconds > :seconds or (r.bestSurvivalSeconds = :seconds and r.playerId < :playerId))
    """)
    long countAheadByStageSurvival(@Param("stageId") int stageId, @Param("seconds") int seconds, @Param("playerId") Long playerId);
}