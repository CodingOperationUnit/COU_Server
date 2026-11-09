package com.couserver.chat.repository;

import com.couserver.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // afterId 이후의 새 메시지 (오래된 순) - 폴링용
    List<ChatMessage> findByPlayerLowIdAndPlayerHighIdAndMessageIdGreaterThanOrderByMessageIdAsc(
            Long lowId, Long highId, Long afterId, Pageable pageable);

    // 처음 입장할 때: 최근 메시지부터 (최신 순, 서비스에서 뒤집어 반환)
    List<ChatMessage> findByPlayerLowIdAndPlayerHighIdOrderByMessageIdDesc(
            Long lowId, Long highId, Pageable pageable);

    // 친구별로 나에게 보낸 마지막 메시지. 쌍에서 내 ID가 낮은 쪽일 때 (idx_chat_pair_id)
    @Query("select m.senderId as senderId, max(m.messageId) as messageId from ChatMessage m "
            + "where m.playerLowId = :playerId and m.senderId <> :playerId group by m.senderId")
    List<LastReceivedMessage> findLastReceivedAsLow(@Param("playerId") Long playerId);

    // 쌍에서 내 ID가 높은 쪽일 때 (idx_chat_high_id)
    @Query("select m.senderId as senderId, max(m.messageId) as messageId from ChatMessage m "
            + "where m.playerHighId = :playerId and m.senderId <> :playerId group by m.senderId")
    List<LastReceivedMessage> findLastReceivedAsHigh(@Param("playerId") Long playerId);
}