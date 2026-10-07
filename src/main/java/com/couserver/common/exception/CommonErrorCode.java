package com.couserver.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


// 공통 에러 코드
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    // 상수 하나 = 에러 하나 : (HTTP 상태, 화면에 보여줄 메시지)
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    // code 문자열은 상수 이름을 그대로 쓴다. (직접 적다가 오타 날 일이 없다)
    @Override
    public String getCode() {
        return name();
    }
}
