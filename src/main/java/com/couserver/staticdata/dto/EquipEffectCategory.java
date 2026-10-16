package com.couserver.staticdata.dto;

// 장비 등급 효과 종류. EquipEffect의 effectCategory
// 서버는 STAT만 계산하고, 나머지는 클라이언트에 데이터로 전달만 한다
public enum EquipEffectCategory {
    STAT,          // 스탯 증가 (70xxx)
    TRIGGER,       // 조건 발동 (71xxx: 피격 시 피해 무효, 주기 회복)
    BUFF,          // 시간제 버프 (72xxx, 2차)
    CONDITIONAL,   // 조건부 효과 (73xxx, 2차)
    NONE           // 제외 대상 (79xxx)
}