package com.couserver.common.exception;

import org.springframework.http.HttpStatus;

// 에러 코드 규격
public interface ErrorCode {

    // HTTP 상태 코드 (예: 404 NOT_FOUND)
    HttpStatus getStatus();

    // 클라이언트가 분기에 쓰는 코드 문자열 (예: "INVENTORY_NOT_FOUND")
    String getCode();

    // 화면에 보여줄 한국어 메시지
    String getMessage();
}
