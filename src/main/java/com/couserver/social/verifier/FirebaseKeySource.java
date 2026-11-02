package com.couserver.social.verifier;

import java.security.PublicKey;

// Firebase ID 토큰 서명 검증용 공개키 조회 (kid → 공개키). 테스트에서 가짜로 바꿔 끼운다
public interface FirebaseKeySource {

    // 해당 kid의 공개키. 없으면 null
    PublicKey find(String kid);
}