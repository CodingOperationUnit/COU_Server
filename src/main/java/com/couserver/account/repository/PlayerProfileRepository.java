package com.couserver.account.repository;

import com.couserver.account.entity.PlayerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerProfileRepository  extends JpaRepository<PlayerProfile, Long> {
    boolean existsByPlayerNickname(String playerNickname);
}
