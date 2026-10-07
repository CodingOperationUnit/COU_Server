package com.couserver.master.repository;

import com.couserver.master.entity.PlayerBaseStat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MasterTableVersionRepository extends JpaRepository<PlayerBaseStat, Long> {
}
