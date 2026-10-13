package com.couserver.friend.dto;

import jakarta.validation.constraints.NotNull;

public record FriendRequestBody(@NotNull(message = "대상 플레이어를 지정해야 합니다.") Long targetPlayerId) {}