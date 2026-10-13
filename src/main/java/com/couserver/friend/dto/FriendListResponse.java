package com.couserver.friend.dto;

import java.util.List;

public record FriendListResponse(List<FriendResponse> friends, int maxFriends) {}
