package com.couserver.friend.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 두 플레이어 사이의 관계
@Getter
@Entity
@Table(name = "friendship",
        uniqueConstraints = @UniqueConstraint(name = "uk_friendship_pair", columnNames = {"player_low_id", "player_high_id"}),
        indexes = @Index(name = "idx_friendship_high", columnList = "player_high_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friendship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long friendshipId;

    @Column(name = "player_low_id", nullable = false)
    private Long playerLowId;

    @Column(name = "player_high_id", nullable = false)
    private Long playerHighId;

    // 요청을 보낸 사람
    @Column(nullable = false)
    private Long requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendshipStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Friendship request(Long requesterId, Long targetId) {
        Friendship friendship = new Friendship();
        friendship.playerLowId = lowOf(requesterId, targetId);
        friendship.playerHighId = highOf(requesterId, targetId);
        friendship.requestedBy = requesterId;
        friendship.status = FriendshipStatus.PENDING;
        friendship.createdAt = LocalDateTime.now();
        return friendship;
    }

    public static Long lowOf(Long a, Long b) {
        return Math.min(a, b);
    }

    public static Long highOf(Long a, Long b) {
        return Math.max(a, b);
    }

    public void accept() {
        this.status = FriendshipStatus.ACCEPTED;
    }

    public boolean isPending() {
        return status == FriendshipStatus.PENDING;
    }

    public boolean involves(Long playerId) {
        return playerLowId.equals(playerId) || playerHighId.equals(playerId);
    }

    // 관계의 상대방
    public Long otherOf(Long playerId) {
        return playerLowId.equals(playerId) ? playerHighId : playerLowId;
    }

    // 요청을 받은 사람
    public Long receiverId() {
        return otherOf(requestedBy);
    }
}