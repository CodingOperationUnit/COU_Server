package com.couserver.chat.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    // 느린 클라이언트 하나 때문에 다른 전송이 막히지 않게 제한을 둔다.
    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 64 * 1024;

    private static final TextMessage CONNECTED = new TextMessage("{\"type\":\"connected\"}");
    private static final TextMessage PONG = new TextMessage("{\"type\":\"pong\"}");

    private final ChatSessionRegistry registry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long playerId = playerIdOf(session);
        if (playerId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        // 같은 세션에 동시에 보내도 깨지지 않도록 감싼다.
        WebSocketSession safeSession =
                new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT_BYTES);
        registry.add(playerId, safeSession);
        safeSession.sendMessage(CONNECTED);
        log.info("[ChatWS] 연결 playerId={} sessionId={} 접속자={}", playerId, session.getId(), registry.connectedPlayerCount());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 클라이언트가 보내는 건 연결 유지용 ping 뿐이다. 무엇이 오든 pong 으로 답한다.
        WebSocketSession target = registeredSession(session);
        target.sendMessage(PONG);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("[ChatWS] 전송 오류 sessionId={} {}", session.getId(), exception.toString());
        session.close(CloseStatus.SERVER_ERROR);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long playerId = playerIdOf(session);
        if (playerId != null) {
            registry.remove(playerId, session.getId());
        }
        log.info("[ChatWS] 종료 playerId={} sessionId={} status={} 접속자={}",
                playerId, session.getId(), status, registry.connectedPlayerCount());
    }

    private WebSocketSession registeredSession(WebSocketSession session) {
        Long playerId = playerIdOf(session);
        if (playerId == null) {
            return session;
        }
        return registry.sessionsOf(playerId).stream()
                .filter(s -> s.getId().equals(session.getId()))
                .findFirst()
                .orElse(session);
    }

    private Long playerIdOf(WebSocketSession session) {
        Object value = session.getAttributes().get(ChatHandshakeInterceptor.PLAYER_ID_ATTRIBUTE);
        return value instanceof Long id ? id : null;
    }
}
