package com.couserver.staticdata.dto;

// 스킬 레벨이 주는 효과 종류. SkillLevel의 effect1Type, effect2Type
public enum SkillEffectType {
    NONE,
    DAMAGE,
    AREA,
    SPEED,
    COOLDOWNREDUCTION,
    RANGE,
    PIERCE,
    CHAIN,
    REPEAT,
    CRITICALCHANCE,
    CRITICALDAMAGE
}