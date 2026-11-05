package com.couserver.friend.dto;

// lastReceivedMessageId: 이 친구가 나에게 보낸 마지막 메시지 ID. 보낸 적이 없으면 0
public record FriendResponse(Long playerId, String nickname, int accountLevel, long combatPower, boolean online,
                             long lastReceivedMessageId) {}
