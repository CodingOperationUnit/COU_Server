package com.couserver.battle.repository;

import com.couserver.battle.entity.BattleSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BattleSessionRepository extends JpaRepository<BattleSession, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from BattleSession s where s.battleId = :battleId")
    Optional<BattleSession> findForUpdate(@Param("battleId") Long battleId);

    // 플레이어의 세션을 모두 지운다
    @Modifying
    @Query("delete from BattleSession s where s.playerId = :playerId")
    int deleteByPlayerId(@Param("playerId") Long playerId);
}
