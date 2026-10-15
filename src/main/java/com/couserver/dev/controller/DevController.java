package com.couserver.dev.controller;

import com.couserver.account.dto.AuthAccount;
import com.couserver.account.dto.CurrencyResponse;
import com.couserver.dev.dto.DevItemGrantRequest;
import com.couserver.dev.service.DevService;
import com.couserver.inventory.dto.EquipmentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "dev-tools.enabled", havingValue = "true")
public class DevController {

    private final DevService devService;

    @PostMapping("/stamina")
    public CurrencyResponse refillStamina(@AuthenticationPrincipal AuthAccount authAccount) {
        return devService.refillStamina(authAccount.getPlayerId());
    }

    @PostMapping("/items")
    public List<EquipmentResponse> grantItems(@AuthenticationPrincipal AuthAccount authAccount,
                                              @Valid @RequestBody DevItemGrantRequest request) {
        return devService.grantItems(authAccount.getPlayerId(), request.itemId(), request.count());
    }
}
