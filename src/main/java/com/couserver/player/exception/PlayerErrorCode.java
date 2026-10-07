package com.couserver.player.exception;

import com.couserver.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlayerErrorCode implements ErrorCode {
    PLAYER_STAT_NOT_FOUND(HttpStatus.NOT_FOUND, "플레이어 스탯 데이터가 없습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode(){
        return name();
    }
}
