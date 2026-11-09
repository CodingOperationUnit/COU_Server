package com.couserver.account.repository;

import com.couserver.account.entity.PlayerProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlayerProfileRepository  extends JpaRepository<PlayerProfile, Long> {
    boolean existsByPlayerNickname(String playerNickname);
    Optional<PlayerProfile> findByAccount_AccountId(Integer accountId);

    // 계정 ID 검색 (친구 추가용)
    @EntityGraph(attributePaths = "account")
    Optional<PlayerProfile> findByAccount_AccountLoginId(String accountLoginId);

    // 닉네임 검색 (친구 추가용)
    @EntityGraph(attributePaths = "account")
    List<PlayerProfile> findTop20ByPlayerNicknameContainingIgnoreCaseOrderByPlayerNicknameAsc(String keyword);

    // 랭킹용: 계정 ID까지 한 번에 읽는다
    @EntityGraph(attributePaths = "account")
    List<PlayerProfile> findByPlayerIdIn(Collection<Long> playerIds);
}
