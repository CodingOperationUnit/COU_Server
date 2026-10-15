package com.couserver.common.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

// 계정(loginId) 기준 로그인 실패 제한. 배포 구성(IP)과 무관하게 동작하는 주된 방어다.
// Redis 장애 시에는 제한을 건너뛴다 (fail-open): 로그인 자체가 막히지 않게 한다
@Slf4j
@Component
public class LoginAttemptLimiter {
    private static final String KEY_PREFIX = "auth:login:fail:";

    private final StringRedisTemplate redis;
    private final int maxFailures;
    private final Duration lockDuration;

    public LoginAttemptLimiter(StringRedisTemplate redis,
                               @Value("${auth.rate-limit.login-max-failures:5}") int maxFailures,
                               @Value("${auth.rate-limit.login-lock-seconds:900}") long lockSeconds) {
        this.redis = redis;
        this.maxFailures = maxFailures;
        this.lockDuration = Duration.ofSeconds(lockSeconds);
    }

    // 잠겨 있으면 남은 초, 아니면 0
    public long lockedSecondsRemaining(String loginId) {
        try {
            String key = key(loginId);
            String count = redis.opsForValue().get(key);
            if (count == null || Long.parseLong(count) < maxFailures) {
                return 0;
            }
            Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
            return (ttl != null && ttl > 0) ? ttl : 0;
        } catch (RuntimeException e) {
            log.warn("로그인 잠금 확인에 실패해 제한 없이 진행합니다.", e);
            return 0;
        }
    }

    // 실패를 1 늘린다. 이번 실패로 잠금이 시작되면 true
    public boolean recordFailure(String loginId) {
        try {
            String key = key(loginId);
            Long count = redis.opsForValue().increment(key);
            if (count == null) {
                return false;
            }
            // 첫 실패에 집계 기간을 정하고, 잠기는 순간 잠금 시간을 처음부터 다시 센다
            if (count == 1 || count == maxFailures) {
                redis.expire(key, lockDuration);
            }
            return count == maxFailures;
        } catch (RuntimeException e) {
            log.warn("로그인 실패 기록에 실패해 제한 없이 진행합니다.", e);
            return false;
        }
    }

    public void reset(String loginId) {
        try {
            redis.delete(key(loginId));
        } catch (RuntimeException e) {
            log.warn("로그인 실패 횟수 초기화에 실패했습니다.", e);
        }
    }

    // DB 정렬 규칙이 대소문자를 구분하지 않아 "ABC"로도 "abc" 계정에 로그인되므로, 키도 소문자로 맞춘다
    private static String key(String loginId) {
        return KEY_PREFIX + loginId.toLowerCase(Locale.ROOT);
    }
}