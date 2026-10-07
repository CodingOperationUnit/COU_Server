package com.couserver.common.exception;



// 에러가 나면 클라이언트에게 보내는 응답 형식
public record ErrorResponse(int status, String code, String message) {

    // ErrorCode(enum 상수) 하나로 응답 객체를 바로 만든다.
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getStatus().value(), errorCode.getCode(), errorCode.getMessage());
    }
}
