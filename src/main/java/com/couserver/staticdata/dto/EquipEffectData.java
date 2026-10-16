package com.couserver.staticdata.dto;

// EquipEffect 시트 (효과 정의). effectId는 7xxxx, 두 번째 자리가 종류이고 9면 제외 대상
// 쓰지 않는 문자열 칸은 None, 숫자 칸은 0
public record EquipEffectData(
        long effectId,
        String effectDescription,
        EquipEffectCategory effectCategory,
        EquipEffectStatType statType,
        EquipEffectValueType valueType,
        double effectValue,
        EquipEffectTriggerType triggerType,
        double triggerChance,
        double triggerInterval,
        EquipEffectActionType actionType,
        double actionValue) {

    // 제외 대상(79xxx)인지. 계산·검증에서 건너뛴다
    public static boolean isExcludedId(long effectId) {
        return (effectId / 1000) % 10 == 9;
    }

    public boolean isExcluded() {
        return isExcludedId(effectId);
    }

    // 계산 대상인 Stat형 효과인지 (제외 아님 + Stat형)
    public boolean isActiveStat() {
        return !isExcluded() && effectCategory == EquipEffectCategory.STAT;
    }
}