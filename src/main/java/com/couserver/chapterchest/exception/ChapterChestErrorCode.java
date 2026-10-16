package com.couserver.chapterchest.exception;

import com.couserver.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 챕터 보물상자 전용 에러 코드
@Getter
@RequiredArgsConstructor
public enum ChapterChestErrorCode implements ErrorCode {
    STAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "스테이지를 찾을 수 없습니다."),
    STAGE_NOT_CLEARED(HttpStatus.BAD_REQUEST, "클리어하지 않은 스테이지입니다."),
    ALREADY_CLAIMED(HttpStatus.CONFLICT, "이미 받은 챕터 보물상자입니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
