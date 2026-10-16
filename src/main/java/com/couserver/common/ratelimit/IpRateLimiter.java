package com.couserver.common.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

// IP 기준 요청 제한 (고정 시간 창). 로그인과 회원가입을 합쳐 센다.
// IP는 request.getRemoteAddr()를 쓰고, 프록시 뒤의 실제 IP 복원은 server.forward-headers-strategy에 맡긴다
@Slf4j
@Component
public class IpRateLimiter {
    private static final String KEY_PREFIX = "auth:ip:";

    private final StringRedisTemplate redis;
    private final int maxRequests;
    private final Duration window;

    public IpRateLimiter(StringRedisTemplate redis,
                         @Value("${auth.rate-limit.ip-max-requests:30}") int maxRequests,
                         @Value("${auth.rate-limit.ip-window-seconds:60}") long windowSeconds) {
        this.redis = redis;
        this.maxRequests = maxRequests;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    // 허용되면 0, 넘었으면 다시 시도할 수 있을 때까지의 초
    public long tryAcquire(String ip) {
        try {
            String key = KEY_PREFIX + ip;
            Long count = redis.opsForValue().increment(key);
            if (count == null) {
                return 0;
            }
            if (count == 1) {
                redis.expire(key, window);
            }
            if (count <= maxRequests) {
                return 0;
            }
            if (count == maxRequests + 1) {
                log.warn("IP 요청 제한을 시작합니다. ip={}", ip);
            }
            Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
            if (ttl == null || ttl < 0) {
                // 증가와 만료 설정 사이에 문제가 생겨 만료 없는 키가 남은 경우: 영구 차단되지 않게 다시 건다
                redis.expire(key, window);
                return window.getSeconds();
            }
            return Math.max(ttl, 1);
        } catch (RuntimeException e) {
            log.warn("IP 요청 제한 확인에 실패해 제한 없이 진행합니다.", e);
            return 0;
        }
    }
}