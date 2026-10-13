package com.couserver.friend.entity;

// 나와 상대의 관계를 나타낸다
public enum FriendRelation {
    NONE,               // 아무 관계 없음
    FRIEND,             // 이미 친구
    REQUEST_SENT,       // 내가 요청을 보낸 상태
    REQUEST_RECEIVED    // 상대가 나에게 요청을 보낸 상태
}