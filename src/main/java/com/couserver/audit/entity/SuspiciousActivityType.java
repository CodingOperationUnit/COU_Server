package com.couserver.audit.entity;

// 의심 활동 유형. 각 단계에서 탐지 지점이 생기면 여기에 추가한다
public enum SuspiciousActivityType {
    SAVE_VALUE_MISMATCH,        // save에 보낸 재화·레벨·스탯·진행도가 서버 값과 다름
    SAVE_LOCKED_STAGE,          // save로 미해금·없는 스테이지 선택
    BATTLE_RESULT_OVER_LIMIT    // 전투 결과가 스테이지 상한을 넘음
}