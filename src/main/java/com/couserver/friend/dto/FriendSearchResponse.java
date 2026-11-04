package com.couserver.friend.dto;


import com.couserver.friend.entity.FriendRelation;

public record FriendSearchResponse(Long playerId, String accountLoginId, String nickname, int accountLevel, FriendRelation relation) {}
