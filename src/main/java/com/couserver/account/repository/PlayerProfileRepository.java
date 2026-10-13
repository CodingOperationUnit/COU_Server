package com.couserver.account.repository;

import com.couserver.account.entity.PlayerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerProfileRepository  extends JpaRepository<PlayerProfile, Long> {
    boolean existsByPlayerNickname(String playerNickname);
    Optional<PlayerProfile> findByAccount_AccountId(Integer accountId);

    // 닉네임 검색 (친구 추가용)
    List<PlayerProfile> findTop20ByPlayerNicknameContainingIgnoreCaseOrderByPlayerNicknameAsc(String keyword);
}
