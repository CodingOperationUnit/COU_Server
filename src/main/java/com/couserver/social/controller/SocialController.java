package com.couserver.social.controller;

import com.couserver.account.dto.AuthAccount;
import com.couserver.social.dto.SocialLinkStatusResponse;
import com.couserver.social.dto.SocialLoginResponse;
import com.couserver.social.dto.SocialTokenRequest;
import com.couserver.social.entity.SocialProvider;
import com.couserver.social.service.SocialAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 구글 플레이 로그인(Firebase ID 토큰)과 계정 연동
// 다른 로그인 방식이 추가되면 같은 모양으로 경로를 하나씩 늘린다 (예: /naver/login)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
public class SocialController {
    private final SocialAuthService socialAuthService;

    // 구글 로그인 (인증 불필요). 연동된 계정이 없으면 새 계정을 만들고 newAccount=true로 응답한다
    @PostMapping("/google/login")
    public ResponseEntity<SocialLoginResponse> googleLogin(@Valid @RequestBody SocialTokenRequest request) {
        return ResponseEntity.ok(socialAuthService.login(SocialProvider.GOOGLE_PLAY_GAMES, request.getIdToken()));
    }

    // 로그인한 계정에 구글 연동
    @PostMapping("/me/google")
    public ResponseEntity<SocialLinkStatusResponse> linkGoogle(@AuthenticationPrincipal AuthAccount authAccount,
                                                               @Valid @RequestBody SocialTokenRequest request) {
        return ResponseEntity.ok(socialAuthService.link(
                authAccount.getAccountId(), SocialProvider.GOOGLE_PLAY_GAMES, request.getIdToken()));
    }

    // 구글 연동 상태
    @GetMapping("/me/google")
    public ResponseEntity<SocialLinkStatusResponse> googleStatus(@AuthenticationPrincipal AuthAccount authAccount) {
        return ResponseEntity.ok(socialAuthService.status(authAccount.getAccountId(), SocialProvider.GOOGLE_PLAY_GAMES));
    }
}