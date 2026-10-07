package com.couserver.account.repository;

import com.couserver.account.entity.StageProgress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StageProgressRepository extends JpaRepository<StageProgress, Long> {
}
