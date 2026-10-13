package com.couserver.friend.repository;

import com.couserver.friend.entity.Friendship;
import com.couserver.friend.entity.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    // 두 사람 사이의 관계
    Optional<Friendship> findByPlayerLowIdAndPlayerHighId(Long playerLowId, Long playerHighId);

    //친구 목록
    @Query("select f from Friendship f where f.status = :status "
            + "and (f.playerLowId = :playerId or f.playerHighId = :playerId)")
    List<Friendship> findByPlayerAndStatus(@Param("playerId") Long playerId,
                                           @Param("status") FriendshipStatus status);

    // 내가 받은 요청
    @Query("select f from Friendship f where f.status = :status and f.requestedBy <> :playerId "
            + "and (f.playerLowId = :playerId or f.playerHighId = :playerId)")
    List<Friendship> findReceivedRequests(@Param("playerId") Long playerId,
                                          @Param("status") FriendshipStatus status);

    // 개수 제한 확인용
    @Query("select count(f) from Friendship f where f.status = :status "
            + "and (f.playerLowId = :playerId or f.playerHighId = :playerId)")
    long countByPlayerAndStatus(@Param("playerId") Long playerId,
                                @Param("status") FriendshipStatus status);

    @Query("select count(f) from Friendship f where f.status = :status and f.requestedBy <> :playerId "
            + "and (f.playerLowId = :playerId or f.playerHighId = :playerId)")
    long countReceivedRequests(@Param("playerId") Long playerId,
                               @Param("status") FriendshipStatus status);
}