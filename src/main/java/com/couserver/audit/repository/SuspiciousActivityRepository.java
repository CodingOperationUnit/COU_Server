package com.couserver.audit.repository;

import com.couserver.audit.entity.SuspiciousActivity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuspiciousActivityRepository extends JpaRepository<SuspiciousActivity, Long> {
}