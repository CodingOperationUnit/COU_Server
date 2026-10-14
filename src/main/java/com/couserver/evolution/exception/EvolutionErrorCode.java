package com.couserver.evolution.exception;

import com.couserver.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 진화 API 에러 코드
@Getter
@RequiredArgsConstructor
public enum EvolutionErrorCode implements ErrorCode {
    CURRENCY_NOT_FOUND(HttpStatus.NOT_FOUND, "재화 데이터가 없습니다."),
    PLAYER_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "플레이어 프로필 데이터가 없습니다."),
    NOT_ENOUGH_GOLD(HttpStatus.BAD_REQUEST, "골드가 부족합니다."),
    EVOLUTION_MAX_STEP_REACHED(HttpStatus.BAD_REQUEST, "더 이상 진화할 수 없습니다."),
    EVOLUTION_LEVEL_LOCKED(HttpStatus.BAD_REQUEST, "계정 레벨이 부족해 더 진화할 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode(){
        return name();
    }
}