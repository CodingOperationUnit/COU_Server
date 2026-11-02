package com.couserver.social.verifier;

import com.couserver.social.entity.SocialProvider;

// 검증이 끝난 소셜 신원. providerUserId는 서버가 토큰에서 직접 꺼낸 값이다 (클라이언트가 보낸 값이 아님)
public record SocialIdentity(SocialProvider provider, String providerUserId) {
}