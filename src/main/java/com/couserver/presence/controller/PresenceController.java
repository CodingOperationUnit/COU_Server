package com.couserver.presence.controller;

import com.couserver.account.dto.AuthAccount;
import com.couserver.presence.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/presence")
@RequiredArgsConstructor
public class PresenceController {
    private final PresenceService presenceService;

    // 클라이언트가 30초마다 호출
    @PostMapping("/heartbeat")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void heartbeat(@AuthenticationPrincipal AuthAccount authAccount) {
        presenceService.heartbeat(authAccount.getPlayerId());
    }
}
