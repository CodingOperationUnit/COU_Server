package com.couserver.account.controller;

import com.couserver.account.dto.AuthAccount;
import com.couserver.account.dto.PlayerSaveDataResponse;
import com.couserver.account.dto.PlayerSaveRequest;
import com.couserver.account.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/players")
public class PlayerController {
    private final PlayerService playerService;

    @GetMapping("/me/save")
    public ResponseEntity<PlayerSaveDataResponse> loadSave(@AuthenticationPrincipal AuthAccount authAccount) {
        return ResponseEntity.ok(playerService.loadSave(authAccount.getPlayerId()));   // ← getPlayerId로 변경
    }

    @PutMapping("/me/save")
    public ResponseEntity<PlayerSaveDataResponse> save(
            @AuthenticationPrincipal AuthAccount authAccount,
            @Valid @RequestBody PlayerSaveRequest request
    ) {
        return ResponseEntity.ok(playerService.save(authAccount.getPlayerId(), request));
    }
}
