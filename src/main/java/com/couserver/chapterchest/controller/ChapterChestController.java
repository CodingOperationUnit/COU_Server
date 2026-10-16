package com.couserver.chapterchest.controller;

import com.couserver.account.dto.AuthAccount;
import com.couserver.chapterchest.dto.ChapterChestClaimResponse;
import com.couserver.chapterchest.service.ChapterChestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chapter-chests")
@RequiredArgsConstructor
public class ChapterChestController {

    private final ChapterChestService chapterChestService;

    @PostMapping("/{stageId}/claim")
    public ChapterChestClaimResponse claim(@AuthenticationPrincipal AuthAccount authAccount,
                                           @PathVariable int stageId) {
        return chapterChestService.claim(authAccount.getPlayerId(), stageId);
    }
}
