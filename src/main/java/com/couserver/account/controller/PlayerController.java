package com.couserver.account.controller;

import com.couserver.account.dto.*;
import com.couserver.account.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;

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


    @GetMapping("/nickname-availability")
    public ResponseEntity<NicknameAvailabilityResponse> checkNicknameAvailability(
            @AuthenticationPrincipal AuthAccount authAccount,
            @RequestParam String nickname
    ) {
        boolean available = playerService.isNicknameAvailable(authAccount.getPlayerId(), nickname);

        return ResponseEntity.ok(
                new NicknameAvailabilityResponse(available)
        );
    }

    @PatchMapping("/me/nickname")
    public ResponseEntity<ChangeNicknameResponse> changeNickname(
            @AuthenticationPrincipal AuthAccount authAccount,
            @Valid @RequestBody ChangeNicknameRequest request
    ) {
        try {
            ChangeNicknameResponse response = playerService.changeNickname(
                    authAccount.getPlayerId(),
                    request.nickname()
            );

            return ResponseEntity.ok(response);

        } catch (DataIntegrityViolationException e) {
            // MySQL 중복 키 오류(1062)만 닉네임 중복으로 변환
            Throwable cause = e;

            while (cause != null) {
                if (cause instanceof SQLException sqlException
                        && sqlException.getErrorCode() == 1062) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "이미 사용 중인 닉네임입니다.",
                            e
                    );
                }
                cause = cause.getCause();
            }

            // 다른 DB 무결성 오류는 숨기지 않는다
            throw e;
        }
    }

}
