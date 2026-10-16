package com.couserver.chat.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 플레이어와 그 세션을 기억한다.
// 서버를 여러 대로 늘리면 Redis Pub/Sub 같은 걸로 서버끼리 이벤트를 전달해야 한다.
@Component
public class ChatSessionRegistry {

    // playerId -> (sessionId -> session)
    private final Map<Long, Map<String, WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public void add(Long playerId, WebSocketSession session) {
        sessions.computeIfAbsent(playerId, id -> new ConcurrentHashMap<>()).put(session.getId(), session);
    }

    public void remove(Long playerId, String sessionId) {
        sessions.computeIfPresent(playerId, (id, byId) -> {
            byId.remove(sessionId);
            return byId.isEmpty() ? null : byId;
        });
    }

    public Collection<WebSocketSession> sessionsOf(Long playerId) {
        Map<String, WebSocketSession> byId = sessions.get(playerId);
        return byId == null ? List.of() : List.copyOf(byId.values());
    }

    public int connectedPlayerCount() {
        return sessions.size();
    }
}
