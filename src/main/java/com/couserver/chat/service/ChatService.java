package com.couserver.chat.service;

import com.couserver.chat.dto.ChatMessageResponse;
import com.couserver.chat.entity.ChatMessage;
import com.couserver.chat.exception.ChatErrorCode;
import com.couserver.chat.repository.ChatMessageRepository;
import com.couserver.chat.websocket.ChatPushService;
import com.couserver.common.exception.BusinessException;
import com.couserver.friend.entity.Friendship;
import com.couserver.friend.entity.FriendshipStatus;
import com.couserver.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_CONTENT_LENGTH = 200;
    private static final int MAX_LIMIT = 100;

    private final ChatMessageRepository chatMessageRepository;
    private final FriendshipRepository friendshipRepository;
    private final ChatPushService chatPushService;
    private final Clock clock;

    // 메시지 보내기
    @Transactional
    public ChatMessageResponse send(Long senderId, Long friendId, String rawContent) {
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isEmpty()) {
            throw new BusinessException(ChatErrorCode.EMPTY_MESSAGE);
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ChatErrorCode.MESSAGE_TOO_LONG);
        }
        validateFriends(senderId, friendId);

        ChatMessage saved = chatMessageRepository.save(
                ChatMessage.of(senderId, friendId, content, Instant.now(clock)));

        pushAfterCommit(friendId, senderId, ChatMessageResponse.of(saved, friendId));

        return ChatMessageResponse.of(saved, senderId);
    }

    // 커밋 전에 push 하면 상대가 afterId 로 조회했을 때 메시지가 아직 안 보일 수 있다.
    private void pushAfterCommit(Long recipientId, Long senderId, ChatMessageResponse message) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            chatPushService.pushMessage(recipientId, senderId, message);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                chatPushService.pushMessage(recipientId, senderId, message);
            }
        });
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long playerId, Long friendId, long afterId, int limit) {
        validateFriends(playerId, friendId);

        Long low = Friendship.lowOf(playerId, friendId);
        Long high = Friendship.highOf(playerId, friendId);
        int size = Math.max(1, Math.min(limit, MAX_LIMIT));

        List<ChatMessage> messages;
        if (afterId <= 0) {
            messages = new ArrayList<>(chatMessageRepository
                    .findByPlayerLowIdAndPlayerHighIdOrderByMessageIdDesc(low, high, PageRequest.of(0, size)));
            Collections.reverse(messages);
        } else {
            messages = chatMessageRepository
                    .findByPlayerLowIdAndPlayerHighIdAndMessageIdGreaterThanOrderByMessageIdAsc(
                            low, high, afterId, PageRequest.of(0, size));
        }

        return messages.stream().map(m -> ChatMessageResponse.of(m, playerId)).toList();
    }

    // 친구(ACCEPTED)끼리만 대화할 수 있다
    private void validateFriends(Long playerId, Long friendId) {
        boolean friends = friendshipRepository
                .findByPlayerLowIdAndPlayerHighId(Friendship.lowOf(playerId, friendId), Friendship.highOf(playerId, friendId))
                .filter(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
                .isPresent();
        if (!friends) {
            throw new BusinessException(ChatErrorCode.NOT_FRIENDS);
        }
    }
}
