package com.couserver.chat.websocket;

import com.couserver.chat.dto.ChatMessageResponse;
import com.couserver.chat.dto.ChatPushEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.util.Collection;

// 접속 중인 상대에게 새 메시지를 바로 밀어준다. 접속 중이 아니면 아무것도 하지 않는다.
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatPushService {

    private final ChatSessionRegistry registry;
    private final ObjectMapper objectMapper;

    // recipientId: 받는 사람, senderId: 보낸 사람
    // message 는 받는 사람 기준으로 만들어서 넘긴다.
    public void pushMessage(Long recipientId, Long senderId, ChatMessageResponse message) {
        Collection<WebSocketSession> targets = registry.sessionsOf(recipientId);
        if (targets.isEmpty()) {
            return;
        }

        TextMessage frame;
        try {
            frame = new TextMessage(objectMapper.writeValueAsString(ChatPushEvent.message(senderId, message)));
        } catch (Exception e) {
            log.warn("[ChatWS] 직렬화 실패 messageId={} {}", message.messageId(), e.toString());
            return;
        }

        for (WebSocketSession session : targets) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(frame);
                }
            } catch (Exception e) {
                // 한 명 전송이 실패해도 메시지 저장(REST 응답)에는 영향을 주지 않는다.
                log.warn("[ChatWS] 전송 실패 recipientId={} sessionId={} {}", recipientId, session.getId(), e.toString());
            }
        }
    }
}
