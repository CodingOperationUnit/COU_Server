package com.couserver.social.verifier;

import com.couserver.social.entity.SocialProvider;

// 소셜 로그인 토큰 검증기. 로그인 방식마다 구현이 하나씩 생긴다 (지금은 구글만)
// 실패하면 BusinessException(SocialErrorCode)을 던진다
public interface SocialIdentityVerifier {

    SocialProvider provider();

    SocialIdentity verify(String token);
}