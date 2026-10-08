package com.couserver.battle.exception;

import com.couserver.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 전투 전용 에러 코드
@Getter
@RequiredArgsConstructor
public enum BattleErrorCode implements ErrorCode {

    STAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "스테이지를 찾을 수 없습니다."),
    STAGE_LOCKED(HttpStatus.BAD_REQUEST, "해금되지 않은 스테이지입니다."),
    NOT_ENOUGH_STAMINA(HttpStatus.BAD_REQUEST, "스태미나가 부족합니다."),
    BATTLE_NOT_FOUND(HttpStatus.NOT_FOUND, "전투를 찾을 수 없습니다."),
    NOT_OWNED_BATTLE(HttpStatus.FORBIDDEN, "내 전투가 아닙니다."),
    BATTLE_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 결과를 반영한 전투입니다."),
    BATTLE_EXPIRED(HttpStatus.CONFLICT, "만료된 전투입니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
