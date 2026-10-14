package com.couserver.chat.controller;

import com.couserver.account.dto.AuthAccount;
import com.couserver.chat.dto.ChatMessageResponse;
import com.couserver.chat.dto.ChatSendRequest;
import com.couserver.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // 메시지 보내기
    @PostMapping("/{friendPlayerId}/messages")
    public ChatMessageResponse send(@AuthenticationPrincipal AuthAccount authAccount,
                                    @PathVariable Long friendPlayerId,
                                    @RequestBody ChatSendRequest body) {
        return chatService.send(authAccount.getPlayerId(), friendPlayerId, body.content());
    }

    // 메시지 조회: afterId=0 이면 최근 메시지, 아니면 afterId 이후의 새 메시지
    @GetMapping("/{friendPlayerId}/messages")
    public List<ChatMessageResponse> getMessages(@AuthenticationPrincipal AuthAccount authAccount,
                                                 @PathVariable Long friendPlayerId,
                                                 @RequestParam(defaultValue = "0") long afterId,
                                                 @RequestParam(defaultValue = "50") int limit) {
        return chatService.getMessages(authAccount.getPlayerId(), friendPlayerId, afterId, limit);
    }
}