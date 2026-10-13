package com.couserver.friend.dto;

public record FriendResponse(Long playerId, String nickname, int accountLevel, long combatPower, boolean online) {}
