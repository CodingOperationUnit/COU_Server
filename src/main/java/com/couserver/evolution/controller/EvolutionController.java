package com.couserver.evolution.controller;

import com.couserver.account.dto.AuthAccount;
import com.couserver.evolution.dto.EvolutionUpgradeResponse;
import com.couserver.evolution.service.EvolutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/players/me/evolution")
public class EvolutionController {
    private final EvolutionService evolutionService;

    @PostMapping("/upgrade")
    public ResponseEntity<EvolutionUpgradeResponse> upgrade(@AuthenticationPrincipal AuthAccount authAccount){
        return ResponseEntity.ok(evolutionService.upgrade(authAccount.getPlayerId()));
    }
}
