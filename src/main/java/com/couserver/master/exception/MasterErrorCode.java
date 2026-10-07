package com.couserver.master.exception;

import com.couserver.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MasterErrorCode implements ErrorCode {
    MAXTER_TABLE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 데이터 테이블입니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode(){
        return name();
    }


}
