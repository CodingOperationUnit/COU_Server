package com.couserver.staticdata.dto;

// Trigger형 효과의 동작. EquipEffect의 actionType
public enum EquipEffectActionType {
    NONE,
    NEGATEDAMAGE,       // 피해 무효
    HEALMAXHPPERCENT    // 최대 체력의 N% 회복
}