package com.couserver.account.service;

import com.couserver.account.dto.AccountResponse;
import com.couserver.account.dto.LoginRequest;
import com.couserver.account.dto.LoginResponse;
import com.couserver.account.dto.SignupRequest;
import com.couserver.account.entity.Account;
import com.couserver.account.repository.AccountRepository;
import com.couserver.account.repository.PlayerProfileRepository;
import com.couserver.audit.entity.SuspiciousActivityType;
import com.couserver.audit.service.SuspiciousActivityRecorder;
import com.couserver.common.exception.RateLimitedException;
import com.couserver.common.ratelimit.LoginAttemptLimiter;
import com.couserver.config.JwtProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountService {
    private static final String DEFAULT_NICKNAME_PREFIX = "플레이어";
    private static final int BCRYPT_MAX_BYTES = 72;   // BCrypt는 72바이트를 넘는 비밀번호를 받지 않는다
    private static final String DUMMY_PASSWORD = "dummy-password-for-timing";

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final PlayerProfileRepository playerProfileRepository;
    private final PlayerService playerService;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final SuspiciousActivityRecorder suspiciousActivityRecorder;

    // 없는 ID로 로그인할 때 비교할 해시. 있는 ID와 비슷한 시간이 걸리게 한다
    private String dummyPasswordHash;

    @PostConstruct
    void initDummyPasswordHash() {
        dummyPasswordHash = passwordEncoder.encode(DUMMY_PASSWORD);
    }

    @Transactional
    public AccountResponse signup(SignupRequest request) {
        if (accountRepository.existsByAccountLoginId(request.getAccountLoginId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다.");
        }
        // 닉네임 중복 검사 제거: accountId가 겹치지 않으므로 기본 닉네임도 겹치지 않는다

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        Account account = new Account(request.getAccountLoginId(), encodedPassword);
        Account savedAccount = accountRepository.save(account);

        String defaultNickname = DEFAULT_NICKNAME_PREFIX + savedAccount.getAccountId();   // 예: "플레이어3"
        playerService.createInitialData(savedAccount, defaultNickname);

        return new AccountResponse(
                savedAccount.getAccountId(),
                savedAccount.getAccountLoginId(),
                savedAccount.getAccountCreatedAt(),
                savedAccount.getAccountLastLoginAt()
        );
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String loginId = request.getAccountLoginId();

        // 1) 잠겨 있으면 비밀번호를 확인하지 않고 거부한다 (잠금 중에는 올바른 비밀번호도 거부)
        long lockedSeconds = loginAttemptLimiter.lockedSecondsRemaining(loginId);
        if (lockedSeconds > 0) {
            throw RateLimitedException.loginLocked(lockedSeconds);
        }

        // 2) 비밀번호 확인. 없는 ID도 같은 비용을 들여, 응답 시간으로 계정 존재를 추측하기 어렵게 한다
        Optional<Account> found = accountRepository.findByAccountLoginId(loginId);
        if (!passwordMatches(request.getPassword(), found)) {
            if (loginAttemptLimiter.recordFailure(loginId)) {
                found.ifPresent(this::recordLoginLocked);
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        // 3) 성공: 실패 횟수 초기화
        loginAttemptLimiter.reset(loginId);
        Account account = found.get();
        account.recordLogin();

        Long playerId = playerService.getPlayerId(account.getAccountId());
        String accessToken = jwtProvider.createAccessToken(account, playerId);
        AccountResponse accountResponse = new AccountResponse(
                account.getAccountId(),
                account.getAccountLoginId(),
                account.getAccountCreatedAt(),
                account.getAccountLastLoginAt()
        );
        return new LoginResponse(accessToken, "Bearer", accountResponse);
    }

    // 계정이 없거나 72바이트를 넘는 입력이면 더미 비교만 하고 실패로 본다.
    // 72바이트를 넘는 입력을 그대로 비교하면 BCrypt가 예외를 던져 500이 된다
    private boolean passwordMatches(String rawPassword, Optional<Account> account) {
        boolean tooLong = rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES;
        String hash = account.map(Account::getAccountPasswordHash).orElse(dummyPasswordHash);
        boolean matched = passwordEncoder.matches(tooLong ? DUMMY_PASSWORD : rawPassword, hash);
        return matched && account.isPresent() && !tooLong;
    }

    private void recordLoginLocked(Account account) {
        playerProfileRepository.findByAccount_AccountId(account.getAccountId())
                .ifPresent(profile -> suspiciousActivityRecorder.record(profile.getPlayerId(),
                        SuspiciousActivityType.LOGIN_LOCKED, "login failures reached the limit"));
    }
}