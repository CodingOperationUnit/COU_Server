package com.couserver.chat.repository;

// 친구가 나에게 보낸 마지막 메시지 (보낸 사람별 가장 큰 messageId)
public interface LastReceivedMessage {
    Long getSenderId();

    Long getMessageId();
}
