package com.couserver.support;

// 테스트에서 만든 계정, 로그인 토큰, 플레이어 ID (DB에 직접 데이터를 넣을 때 쓴다)
public record TestAccount(String loginId, String accessToken, Long playerId) {
}