package com.couserver.social.entity;

// 소셜 로그인 방식. 값을 추가해도 DB 컬럼이 varchar라 ddl-auto로 반영된다
public enum SocialProvider {
    GOOGLE_PLAY_GAMES   // Play Games 로그인 → Firebase Auth (신원 값 = Firebase UID)
}