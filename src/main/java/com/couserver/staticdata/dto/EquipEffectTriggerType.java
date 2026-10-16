package com.couserver.staticdata.dto;

// Trigger형 효과의 발동 조건. EquipEffect의 triggerType
public enum EquipEffectTriggerType {
    NONE,
    ONDAMAGED,   // 피격 시
    INTERVAL     // 일정 주기마다
}