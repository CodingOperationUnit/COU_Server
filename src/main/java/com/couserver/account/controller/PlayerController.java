package com.couserver.account.controller;

import com.couserver.account.dto.AuthAccount;
import com.couserver.account.dto.PlayerSaveDataResponse;
import com.couserver.account.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/players")
public class PlayerController {
    private final PlayerService playerService;

    @GetMapping("/me/save")
    public ResponseEntity<PlayerSaveDataResponse> loadSave(@AuthenticationPrincipal AuthAccount authAccount) {
        return ResponseEntity.ok(playerService.loadSave(authAccount.getAccountId()));
    }
}
