package com.couserver.account;

import com.couserver.support.IntegrationTestSupport;
import com.couserver.support.TestAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 2단계: 로그인·회원가입 시도 제한과 비밀번호 정책 (설계안 4.4)
class AuthRateLimitTest extends IntegrationTestSupport {
    private static final String LOGIN_FAIL_KEY_PREFIX = "auth:login:fail:";
    private static final String WRONG_PASSWORD = "wrong-password-0000";

    @Value("${auth.rate-limit.login-max-failures:5}")
    private int maxFailures;

    @Value("${auth.rate-limit.login-lock-seconds:900}")
    private long lockSeconds;

    @Value("${auth.rate-limit.ip-max-requests:30}")
    private int ipMaxRequests;

    @Autowired
    private StringRedisTemplate redis;

    @Test
    @DisplayName("같은 ID로 정해진 횟수만큼 틀리면 잠기고, 잠금 중에는 올바른 비밀번호도 거부된다 (F4)")
    void lockAfterRepeatedFailures() throws Exception {
        String loginId = uniqueLoginId();
        signup(loginId, TEST_PASSWORD).andExpect(status().isCreated());

        for (int i = 0; i < maxFailures; i++) {
            login(loginId, WRONG_PASSWORD).andExpect(status().isUnauthorized());
        }

        login(loginId, WRONG_PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"))
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
        login(loginId, TEST_PASSWORD)
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("대소문자를 바꾼 ID로도 잠금을 피할 수 없다")
    void lockIgnoresLoginIdCase() throws Exception {
        String loginId = uniqueLoginId();
        signup(loginId, TEST_PASSWORD).andExpect(status().isCreated());
        lock(loginId);

        login(loginId.toUpperCase(), TEST_PASSWORD)
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("잠금이 만료되면 다시 로그인할 수 있다")
    void unlockAfterLockExpires() throws Exception {
        String loginId = uniqueLoginId();
        signup(loginId, TEST_PASSWORD).andExpect(status().isCreated());
        lock(loginId);

        String key = LOGIN_FAIL_KEY_PREFIX + loginId;
        Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
        assertThat(ttl).isPositive().isLessThanOrEqualTo(lockSeconds);

        redis.delete(key);   // 잠금 만료를 대신한다
        login(loginId, TEST_PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그인에 성공하면 실패 횟수가 초기화된다")
    void successResetsFailureCount() throws Exception {
        String loginId = uniqueLoginId();
        signup(loginId, TEST_PASSWORD).andExpect(status().isCreated());

        for (int round = 0; round < 2; round++) {
            for (int i = 0; i < maxFailures - 1; i++) {
                login(loginId, WRONG_PASSWORD).andExpect(status().isUnauthorized());
            }
            login(loginId, TEST_PASSWORD).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("같은 IP의 로그인과 회원가입을 합쳐 제한하고, 다른 IP에는 영향이 없다 (F9)")
    void ipLimitCoversLoginAndSignup() throws Exception {
        String ip = randomIp();

        for (int i = 0; i < ipMaxRequests - 1; i++) {
            login(uniqueLoginId(), TEST_PASSWORD, ip).andExpect(status().isUnauthorized());
        }
        signup(uniqueLoginId(), TEST_PASSWORD, ip).andExpect(status().isCreated());   // 로그인과 함께 센다

        login(uniqueLoginId(), TEST_PASSWORD, ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"))
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
        signup(uniqueLoginId(), TEST_PASSWORD, ip)
                .andExpect(status().isTooManyRequests());

        login(uniqueLoginId(), TEST_PASSWORD, randomIp())
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "[{index}] 길이 {0}")
    @MethodSource("invalidPasswords")
    @DisplayName("정책에 맞지 않는 비밀번호로는 가입할 수 없다 (F7)")
    void signupRejectsInvalidPassword(String password) throws Exception {
        signup(uniqueLoginId(), password)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    static Stream<String> invalidPasswords() {
        return Stream.of(
                "a",
                "abcdefg",           // 7자
                "a".repeat(65),      // 65자
                "가".repeat(25));    // 25자지만 UTF-8 75바이트
    }

    @Test
    @DisplayName("8자 비밀번호로는 가입할 수 있다")
    void signupAcceptsEightCharacters() throws Exception {
        signup(uniqueLoginId(), "abcd1234").andExpect(status().isCreated());
    }

    @Test
    @DisplayName("지나치게 긴 로그인 비밀번호는 500이 아니라 400이다")
    void tooLongLoginPasswordIsRejected() throws Exception {
        String loginId = uniqueLoginId();
        signup(loginId, TEST_PASSWORD).andExpect(status().isCreated());

        login(loginId, "a".repeat(200))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("계정이 잠기면 의심 활동으로 한 번 기록된다")
    void lockIsRecordedAsSuspiciousActivity() throws Exception {
        TestAccount account = createAccount();
        lock(account.loginId());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM suspicious_activity WHERE player_id = ? AND activity_type = 'LOGIN_LOCKED'",
                        Integer.class, account.playerId()))
                        .isEqualTo(1));
    }

    // 정해진 횟수만큼 틀려 잠금을 건다
    private void lock(String loginId) throws Exception {
        for (int i = 0; i < maxFailures; i++) {
            login(loginId, WRONG_PASSWORD).andExpect(status().isUnauthorized());
        }
        login(loginId, WRONG_PASSWORD).andExpect(status().isTooManyRequests());
    }
}