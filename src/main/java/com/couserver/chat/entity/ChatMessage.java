package com.couserver.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

// 친구 사이의 1:1 채팅 메시지
@Getter
@Entity
@Table(name = "chat_message",
        indexes = @Index(name = "idx_chat_pair_id", columnList = "player_low_id, player_high_id, messageId"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    @Column(name = "player_low_id", nullable = false)
    private Long playerLowId;

    @Column(name = "player_high_id", nullable = false)
    private Long playerHighId;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false, length = 200)
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static ChatMessage of(Long senderId, Long receiverId, String content, Instant now) {
        ChatMessage message = new ChatMessage();
        message.playerLowId = Math.min(senderId, receiverId);
        message.playerHighId = Math.max(senderId, receiverId);
        message.senderId = senderId;
        message.content = content;
        message.createdAt = now;
        return message;
    }
}