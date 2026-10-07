package com.couserver.account.repository;

import com.couserver.account.entity.PlayerStat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerStatRepository extends JpaRepository<PlayerStat, Long> {
}
