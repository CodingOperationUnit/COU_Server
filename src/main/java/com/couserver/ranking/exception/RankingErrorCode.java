package com.couserver.ranking.exception;

import com.couserver.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 랭킹 전용 에러 코드
@Getter
@RequiredArgsConstructor
public enum RankingErrorCode implements ErrorCode {

    INVALID_RANKING_TYPE(HttpStatus.BAD_REQUEST, "올바르지 않은 랭킹 종류입니다."),
    UNSUPPORTED_RANKING_TYPE(HttpStatus.BAD_REQUEST, "아직 지원하지 않는 랭킹 종류입니다."),
    STAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 스테이지입니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
