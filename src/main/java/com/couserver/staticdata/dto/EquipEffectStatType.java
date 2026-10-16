package com.couserver.staticdata.dto;

// Stat형 효과가 올리는 스탯. EquipEffect의 statType
// 서버 최종 스탯(E2)에 반영하는 것은 HP, ATTACK, DEFENSE뿐이다
public enum EquipEffectStatType {
    NONE,
    HP,                 // 최대 체력 %
    ATTACK,             // 공격력 %
    DEFENSE,            // 방어력 고정값
    MOVE_SPEED,         // 이동속도 %
    CRITICAL_CHANCE,    // 치명타 확률 %p
    CRITICAL_DAMAGE,    // 치명타 피해 %p
    DODGE_CHANCE,       // 회피 확률 %p
    DAMAGE_REDUCTION,   // 받는 피해 감소 %p
    HEAL_RECEIVED;      // 받는 치유량 %p

    // 이 스탯이 가져야 하는 값 종류 (명세 3장 Stat형 규칙)
    public EquipEffectValueType requiredValueType() {
        return switch (this) {
            case NONE -> EquipEffectValueType.NONE;
            case HP, ATTACK, MOVE_SPEED -> EquipEffectValueType.PERCENT;
            case DEFENSE, CRITICAL_CHANCE, CRITICAL_DAMAGE,
                 DODGE_CHANCE, DAMAGE_REDUCTION, HEAL_RECEIVED -> EquipEffectValueType.ADD;
        };
    }
}