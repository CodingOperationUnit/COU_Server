package com.couserver.chat.dto;

import com.couserver.chat.entity.ChatMessage;

import java.time.Instant;

public record ChatMessageResponse(Long messageId, Long senderId, boolean mine, String content, Instant createdAt) {

    public static ChatMessageResponse of(ChatMessage message, Long viewerId) {
        return new ChatMessageResponse(
                message.getMessageId(),
                message.getSenderId(),
                message.getSenderId().equals(viewerId),
                message.getContent(),
                message.getCreatedAt());
    }
}
