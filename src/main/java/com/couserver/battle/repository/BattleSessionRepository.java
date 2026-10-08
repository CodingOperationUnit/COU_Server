package com.couserver.battle.repository;

import com.couserver.battle.entity.BattleSession;
import com.couserver.battle.entity.BattleStatus;
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

    // 플레이어의 from 상태 세션을 모두 to 상태로 바꾼다
    @Modifying
    @Query("update BattleSession s set s.status = :to where s.playerId = :playerId and s.status = :from")
    int updateStatus(@Param("playerId") Long playerId,
                     @Param("from") BattleStatus from,
                     @Param("to") BattleStatus to);
}
