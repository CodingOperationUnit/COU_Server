package com.couserver.battle.controller;

import com.couserver.account.dto.AuthAccount;
import com.couserver.battle.dto.BattleEnterRequest;
import com.couserver.battle.dto.BattleEnterResponse;
import com.couserver.battle.dto.BattleResultRequest;
import com.couserver.battle.dto.BattleResultResponse;
import com.couserver.battle.service.BattleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/battles")
@RequiredArgsConstructor
public class BattleController {

    private final BattleService battleService;

    @PostMapping
    public BattleEnterResponse enter(@AuthenticationPrincipal AuthAccount authAccount,
                                     @Valid @RequestBody BattleEnterRequest request) {
        return battleService.enter(authAccount.getPlayerId(), request.stageId());
    }

    @PostMapping("/{battleId}/result")
    public BattleResultResponse submitResult(@AuthenticationPrincipal AuthAccount authAccount,
                                             @PathVariable Long battleId,
                                             @Valid @RequestBody BattleResultRequest request) {
        return battleService.submitResult(authAccount.getPlayerId(), battleId, request);
    }
}
