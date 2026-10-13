package com.couserver.friend.controller;


import com.couserver.account.dto.AuthAccount;
import com.couserver.friend.dto.*;
import com.couserver.friend.service.FriendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    // 친구 목록
    @GetMapping
    public FriendListResponse getFriends(@AuthenticationPrincipal AuthAccount authAccount) {
        return friendService.getFriends(authAccount.getPlayerId());
    }

    // 받은 친구 요청 목록
    @GetMapping("/requests")
    public List<FriendRequestResponse> getReceivedRequests(@AuthenticationPrincipal AuthAccount authAccount) {
        return friendService.getReceivedRequests(authAccount.getPlayerId());
    }

    // 친구 요청 보내기
    @PostMapping("/requests")
    public FriendRequestResultResponse sendRequest(@AuthenticationPrincipal AuthAccount authAccount,
                                                   @Valid @RequestBody FriendRequestBody body) {
        return friendService.sendRequest(authAccount.getPlayerId(), body.targetPlayerId());
    }

    // 요청 수락
    @PostMapping("/requests/{requestId}/accept")
    public FriendResponse accept(@AuthenticationPrincipal AuthAccount authAccount, @PathVariable Long requestId) {
        return friendService.acceptRequest(authAccount.getPlayerId(), requestId);
    }

    // 요청 거절
    @PostMapping("/requests/{requestId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@AuthenticationPrincipal AuthAccount authAccount, @PathVariable Long requestId) {
        friendService.rejectRequest(authAccount.getPlayerId(), requestId);
    }

    // 친구 삭제
    @DeleteMapping("/{friendPlayerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal AuthAccount authAccount, @PathVariable Long friendPlayerId) {
        friendService.removeFriend(authAccount.getPlayerId(), friendPlayerId);
    }

    // 플레이어 검색 (닉네임 또는 플레이어 ID)
    @GetMapping("/search")
    public List<FriendSearchResponse> search(@AuthenticationPrincipal AuthAccount authAccount,
                                             @RequestParam String keyword) {
        return friendService.search(authAccount.getPlayerId(), keyword);
    }
}
