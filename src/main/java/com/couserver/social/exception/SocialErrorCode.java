package com.couserver.social.exception;

import com.couserver.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 소셜 로그인·연동 에러 코드. 메시지에 토큰이나 키 값을 넣지 않는다
@Getter
@RequiredArgsConstructor
public enum SocialErrorCode implements ErrorCode {
    SOCIAL_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "소셜 로그인 정보가 올바르지 않습니다. 다시 로그인해 주세요."),
    GOOGLE_LOGIN_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "구글 로그인이 아직 설정되지 않았습니다."),
    SOCIAL_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "로그인 서비스에 일시적으로 연결할 수 없습니다. 잠시 후 다시 시도해 주세요."),
    SOCIAL_IDENTITY_ALREADY_LINKED(HttpStatus.CONFLICT, "이 소셜 계정은 이미 다른 게임 계정에 연동되어 있습니다."),
    SOCIAL_PROVIDER_ALREADY_LINKED(HttpStatus.CONFLICT, "이 게임 계정에는 이미 다른 소셜 계정이 연동되어 있습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}