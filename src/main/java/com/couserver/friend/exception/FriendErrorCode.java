package com.couserver.friend.exception;

import com.couserver.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 친구 전용 에러 코드
@Getter
@RequiredArgsConstructor
public enum FriendErrorCode implements ErrorCode {
    CANNOT_REQUEST_SELF(HttpStatus.BAD_REQUEST, "자기 자신에게는 친구 요청을 보낼 수 없습니다."),
    PLAYER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 플레이어입니다."),
    ALREADY_FRIENDS(HttpStatus.CONFLICT, "이미 친구입니다."),
    REQUEST_ALREADY_SENT(HttpStatus.CONFLICT, "이미 친구 요청을 보냈습니다."),
    REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "친구 요청을 찾을 수 없습니다."),
    NOT_REQUEST_RECEIVER(HttpStatus.FORBIDDEN, "받은 요청만 수락하거나 거절할 수 있습니다."),
    NOT_FRIENDS(HttpStatus.NOT_FOUND, "친구가 아닙니다."),
    FRIEND_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "친구는 최대 50명까지 만들 수 있습니다."),
    TARGET_FRIEND_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "상대방의 친구 수가 가득 찼습니다."),
    TARGET_REQUEST_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "상대방이 받을 수 있는 친구 요청이 가득 찼습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
