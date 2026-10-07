package com.couserver.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    // 어떤 에러인지 (상태 코드, code, message를 담고 있다)
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
