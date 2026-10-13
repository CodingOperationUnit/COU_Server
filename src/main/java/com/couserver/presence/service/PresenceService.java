package com.couserver.presence.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

// 접속 여부: heartbeat가 올 때마다 키를 갱신하고, 끊기면 TTL로 자동 만료된다
@Service
@RequiredArgsConstructor
public class PresenceService {
    private static final String KEY_PREFIX = "presence:";
    private static final Duration TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redis;

    public void heartbeat(Long playerId) {
        redis.opsForValue().set(KEY_PREFIX + playerId, "1", TTL);
    }

    public boolean isOnline(Long playerId) {
        return Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + playerId));
    }

    // 여러 명을 한 번에 확인 (친구 목록용)
    public Set<Long> onlineAmong(Collection<Long> playerIds) {
        if (playerIds.isEmpty()) {
            return Set.of();
        }
        List<Long> ids = new ArrayList<>(playerIds);
        List<String> values = redis.opsForValue().multiGet(ids.stream().map(id -> KEY_PREFIX + id).toList());

        Set<Long> online = new HashSet<>();
        if (values != null) {
            for (int i = 0; i < ids.size(); i++) {
                if (values.get(i) != null) {
                    online.add(ids.get(i));
                }
            }
        }
        return online;
    }
}
