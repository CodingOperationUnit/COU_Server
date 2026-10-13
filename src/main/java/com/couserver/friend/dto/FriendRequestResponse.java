package com.couserver.friend.dto;


public record FriendRequestResponse(Long requestId, Long playerId, String nickname, int accountLevel) {}
