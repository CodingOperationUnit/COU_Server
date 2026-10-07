package com.couserver.player.controller;

import com.couserver.account.dto.AuthAccount;
import com.couserver.player.dto.PlayerFinalStatResponse;
import com.couserver.player.service.PlayerFinalStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/players/me/stats")
public class PlayerStatController {
    private final PlayerFinalStatService playerFinalStatService;

    @GetMapping
    public ResponseEntity<PlayerFinalStatResponse> getFinalStat(@AuthenticationPrincipal AuthAccount authAccount){
        return ResponseEntity.ok(playerFinalStatService.getFinalStat(authAccount.getPlayerId()));
    }
}
