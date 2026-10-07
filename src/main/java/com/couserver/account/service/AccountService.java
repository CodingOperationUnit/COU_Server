package com.couserver.account.service;

import com.couserver.account.dto.AccountResponse;
import com.couserver.account.dto.LoginRequest;
import com.couserver.account.dto.LoginResponse;
import com.couserver.account.dto.SignupRequest;
import com.couserver.account.entity.Account;
import com.couserver.account.entity.PlayerProfile;
import com.couserver.account.repository.AccountRepository;
import com.couserver.account.repository.PlayerProfileRepository;
import com.couserver.config.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final PlayerProfileRepository playerProfileRepository;
    private final PlayerService playerService;

    @Transactional
    public AccountResponse signup(SignupRequest request) {
        if (accountRepository.existsByAccountLoginId(request.getAccountLoginId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다.");
        }
        if (playerProfileRepository.existsByPlayerNickname(request.getPlayerNickname())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        Account account = new Account(request.getAccountLoginId(), encodedPassword);
        Account savedAccount = accountRepository.save(account);

        playerService.createInitialData(savedAccount, request.getPlayerNickname());

        return new AccountResponse(
                savedAccount.getAccountId(),
                savedAccount.getAccountLoginId(),
                savedAccount.getAccountCreatedAt(),
                savedAccount.getAccountLastLoginAt()
        );
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Account account = accountRepository.findByAccountLoginId(request.getAccountLoginId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), account.getAccountPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

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
}
