package com.couserver.account.controller;

import com.couserver.account.dto.AuthAccount;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    @GetMapping("/me/save")
    public ResponseEntity<String> loadSave(@AuthenticationPrincipal AuthAccount authAccount) {
        return ResponseEntity.ok("accountId = " + authAccount.getAccountId());
    }
}
