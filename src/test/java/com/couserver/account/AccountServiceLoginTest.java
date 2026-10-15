package com.couserver.account;

import com.couserver.account.dto.LoginRequest;
import com.couserver.account.entity.Account;
import com.couserver.account.repository.AccountRepository;
import com.couserver.account.repository.PlayerProfileRepository;
import com.couserver.account.service.AccountService;
import com.couserver.account.service.PlayerService;
import com.couserver.audit.service.SuspiciousActivityRecorder;
import com.couserver.common.ratelimit.LoginAttemptLimiter;
import com.couserver.config.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceLoginTest {
    private static final String DUMMY_HASH = "dummy-hash";

    @Mock private AccountRepository accountRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtProvider jwtProvider;
    @Mock private PlayerProfileRepository playerProfileRepository;
    @Mock private PlayerService playerService;
    @Mock private LoginAttemptLimiter loginAttemptLimiter;
    @Mock private SuspiciousActivityRecorder suspiciousActivityRecorder;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository, passwordEncoder, jwtProvider,
                playerProfileRepository, playerService, loginAttemptLimiter, suspiciousActivityRecorder);
        when(passwordEncoder.encode(anyString())).thenReturn(DUMMY_HASH);
        ReflectionTestUtils.invokeMethod(accountService, "initDummyPasswordHash");
    }

    @Test
    @DisplayName("없는 ID도 더미 해시로 비밀번호 비교를 한 번 수행하고 실패를 기록한다")
    void unknownLoginIdStillComparesPassword() {
        when(accountRepository.findByAccountLoginId("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.login(request("ghost", "some-password")))
                .isInstanceOf(ResponseStatusException.class);

        verify(passwordEncoder).matches("some-password", DUMMY_HASH);
        verify(loginAttemptLimiter).recordFailure("ghost");
    }

    @Test
    @DisplayName("72바이트를 넘는 비밀번호는 BCrypt에 그대로 넘기지 않고 실패로 처리한다")
    void tooLongPasswordIsNotPassedToBcrypt() {
        Account account = new Account("user1", "real-hash");
        when(accountRepository.findByAccountLoginId("user1")).thenReturn(Optional.of(account));
        String tooLong = "가".repeat(30);   // 90바이트

        assertThatThrownBy(() -> accountService.login(request("user1", tooLong)))
                .isInstanceOf(ResponseStatusException.class);

        verify(passwordEncoder, never()).matches(eq(tooLong), anyString());
        verify(passwordEncoder).matches(
                argThat(raw -> raw.toString().getBytes(StandardCharsets.UTF_8).length <= 72), eq("real-hash"));
    }

    private static LoginRequest request(String loginId, String password) {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "accountLoginId", loginId);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }
}