package com.couserver.achievement.controller;

import com.couserver.account.dto.AuthAccount;
import com.couserver.achievement.dto.AchievementClaimResponse;
import com.couserver.achievement.dto.PlayerAchievementResponse;
import com.couserver.achievement.service.AchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/players/me/achievements")
public class AchievementController {
    private final AchievementService achievementService;

    // 내 업적 진행도 전체
    @GetMapping
    public ResponseEntity<List<PlayerAchievementResponse>> getAchievements (
            @AuthenticationPrincipal AuthAccount authAccount) {
        return ResponseEntity.ok(achievementService.getAchievements(authAccount.getPlayerId()));
    }

    // 다음 단계 보상 받기
    @PostMapping("/{achievementId}/claim")
    public ResponseEntity<AchievementClaimResponse> claim(
            @AuthenticationPrincipal AuthAccount authAccount,
            @PathVariable int achievementId) {
        return ResponseEntity.ok(achievementService.claim(authAccount.getPlayerId(), achievementId));
    }
}
