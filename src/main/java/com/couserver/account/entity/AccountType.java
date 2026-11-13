package com.couserver.account.entity;

// 계정 종류. 바꾸는 API는 없고 운영 DB에서 직접 바꾼다. 종류가 늘면 여기에 추가하고 규칙 메서드를 정한다
public enum AccountType {
    NORMAL,     // 일반 플레이어
    TEST;       // 테스트용 계정 (마스터 계정 등)

    // 랭킹에 올리는 계정인지
    public boolean isRanked() {
        return this == NORMAL;
    }
}
