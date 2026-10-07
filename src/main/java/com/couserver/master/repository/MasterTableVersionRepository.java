package com.couserver.master.repository;

import com.couserver.master.entity.MasterTableVersion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MasterTableVersionRepository extends JpaRepository<MasterTableVersion, String> {
}
