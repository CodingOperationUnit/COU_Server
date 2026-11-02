package com.couserver.social.dto;

import com.couserver.account.dto.AccountResponse;
import com.couserver.account.dto.LoginResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 기존 로그인 응답과 같은 필드 + newAccount (이번 로그인으로 계정이 새로 만들어졌는지)
@Getter
@RequiredArgsConstructor
public class SocialLoginResponse {
    private final String accessToken;
    private final String tokenType;
    private final AccountResponse account;
    private final boolean newAccount;

    public static SocialLoginResponse of(LoginResponse login, boolean newAccount) {
        return new SocialLoginResponse(login.getAccessToken(), login.getTokenType(), login.getAccount(), newAccount);
    }
}