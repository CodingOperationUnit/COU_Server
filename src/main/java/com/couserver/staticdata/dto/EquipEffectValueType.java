package com.couserver.staticdata.dto;

// Stat형 효과 수치의 적용 방식. EquipEffect의 valueType
public enum EquipEffectValueType {
    NONE,
    PERCENT,   // 곱해지는 보너스%에 더함
    ADD        // 값에 그대로 더함
}