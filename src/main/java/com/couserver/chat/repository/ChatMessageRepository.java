package com.couserver.chat.repository;

import com.couserver.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // afterId 이후의 새 메시지 (오래된 순) - 폴링용
    List<ChatMessage> findByPlayerLowIdAndPlayerHighIdAndMessageIdGreaterThanOrderByMessageIdAsc(
            Long lowId, Long highId, Long afterId, Pageable pageable);

    // 처음 입장할 때: 최근 메시지부터 (최신 순, 서비스에서 뒤집어 반환)
    List<ChatMessage> findByPlayerLowIdAndPlayerHighIdOrderByMessageIdDesc(
            Long lowId, Long highId, Pageable pageable);
}