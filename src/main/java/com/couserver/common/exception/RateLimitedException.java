package com.couserver.common.exception;

import lombok.Getter;

// 시도 제한에 걸렸을 때. 응답은 429, Retry-After 헤더, 남은 시간이 담긴 메시지
@Getter
public class RateLimitedException extends RuntimeException {
    private final long retryAfterSeconds;

    private RateLimitedException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public static RateLimitedException loginLocked(long retryAfterSeconds) {
        long minutes = Math.max(1, (retryAfterSeconds + 59) / 60);
        return new RateLimitedException(
                "로그인 시도가 너무 많습니다. " + minutes + "분 후 다시 시도해 주세요.", retryAfterSeconds);
    }

    public static RateLimitedException tooManyRequests(long retryAfterSeconds) {
        return new RateLimitedException(
                "요청이 너무 많습니다. " + retryAfterSeconds + "초 후 다시 시도해 주세요.", retryAfterSeconds);
    }

    public ErrorResponse toErrorResponse() {
        CommonErrorCode code = CommonErrorCode.TOO_MANY_REQUESTS;
        return new ErrorResponse(code.getStatus().value(), code.getCode(), getMessage());
    }
}