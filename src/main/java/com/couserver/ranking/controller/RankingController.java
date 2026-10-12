package com.couserver.ranking.controller;

import com.couserver.account.dto.AuthAccount;
import com.couserver.ranking.dto.RankingResponse;
import com.couserver.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    // 랭킹 조회 (계정 레벨, 최고 클리어 스테이지, 스테이지별 생존 시간)
    @GetMapping
    public RankingResponse getRanking(@AuthenticationPrincipal AuthAccount authAccount,
                                      @RequestParam String type,
                                      @RequestParam(required = false) Integer stageId,
                                      @RequestParam(required = false) Integer limit) {
        return rankingService.getRanking(authAccount.getPlayerId(), type, stageId, limit);
    }
}
