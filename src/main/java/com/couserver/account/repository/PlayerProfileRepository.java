package com.couserver.account.repository;

import com.couserver.account.entity.AccountType;
import com.couserver.account.entity.PlayerProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 랭킹 백필용: 계정 종류까지 한 번에 읽는다
    @Query("select p from PlayerProfile p join fetch p.account")
    List<PlayerProfile> findAllWithAccount();

    // 랭킹 반영 여부 확인용
    @Query("select p.account.accountType from PlayerProfile p where p.playerId = :playerId")
    Optional<AccountType> findAccountTypeByPlayerId(@Param("playerId") Long playerId);
}
