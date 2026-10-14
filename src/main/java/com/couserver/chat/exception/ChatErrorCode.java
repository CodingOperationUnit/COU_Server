package com.couserver.chat.exception;

import com.couserver.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements ErrorCode {
    NOT_FRIENDS(HttpStatus.FORBIDDEN, "친구에게만 메시지를 보낼 수 있습니다."),
    EMPTY_MESSAGE(HttpStatus.BAD_REQUEST, "메시지를 입력해주세요."),
    MESSAGE_TOO_LONG(HttpStatus.BAD_REQUEST, "메시지는 200자까지 보낼 수 있습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}