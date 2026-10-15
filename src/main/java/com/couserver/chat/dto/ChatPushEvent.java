package com.couserver.chat.dto;

// WebSocket 으로 클라이언트에게 밀어주는 이벤트.
// type: "message" (새 메시지), friendId: 대화 상대(= 메시지를 보낸 사람), message: 받는 사람 기준(mine=false)
public record ChatPushEvent(String type, Long friendId, ChatMessageResponse message) {

    public static ChatPushEvent message(Long friendId, ChatMessageResponse message) {
        return new ChatPushEvent("message", friendId, message);
    }
}
