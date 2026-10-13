package com.couserver.achievement.exception;

import com.couserver.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AchievementErrorCode implements ErrorCode {
    ACHIEVEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 업적입니다."),
    NOTHING_TO_CLAIM(HttpStatus.BAD_REQUEST, "받을 수 있는 업적 보상이 없습니다."),
    CURRENCY_NOT_FOUND(HttpStatus.NOT_FOUND, "재화 데이터가 없습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
