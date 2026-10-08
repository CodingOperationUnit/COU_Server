package com.couserver.account.repository;

import com.couserver.account.entity.Currency;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    // 전투 입장·결과는 이 행을 먼저 잠가 플레이어 단위로 차례대로 처리한다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Currency c where c.playerId = :playerId")
    Optional<Currency> findForUpdate(@Param("playerId") Long playerId);
}
