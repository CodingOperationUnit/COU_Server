package com.couserver.player.service;

import com.couserver.inventory.entity.ItemGrade;
import com.couserver.player.service.EquipmentEffectCalculator.EquipmentEffectSum;
import com.couserver.staticdata.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EquipmentEffectCalculatorTest {

    private static final long ITEM = 1003;

    private static EquipEffectData stat(long id, EquipEffectStatType type, EquipEffectValueType valueType, double value) {
        return new EquipEffectData(id, "테스트", EquipEffectCategory.STAT, type, valueType, value,
                EquipEffectTriggerType.NONE, 0, 0, EquipEffectActionType.NONE, 0);
    }

    private static EquipEffectData other(long id, EquipEffectCategory category) {
        return new EquipEffectData(id, "테스트", category, EquipEffectStatType.NONE, EquipEffectValueType.NONE, 0,
                EquipEffectTriggerType.NONE, 0, 0, EquipEffectActionType.NONE, 0);
    }

    // 1003 퀀텀 실드 아머와 같은 구성: General 체력 +10%, Super 체력 +15%, Rare 방어력 +12·받는 피해 감소 8%
    // 여기에 제외 효과와 Trigger형을 섞어 둔다
    private static EquipmentEffectCalculator calculator() {
        Map<ItemGrade, List<EquipEffectData>> byGrade = new EnumMap<>(ItemGrade.class);
        byGrade.put(ItemGrade.GENERAL, List.of(
                stat(70001, EquipEffectStatType.HP, EquipEffectValueType.PERCENT, 10)));
        byGrade.put(ItemGrade.SUPER, List.of(
                stat(70002, EquipEffectStatType.HP, EquipEffectValueType.PERCENT, 15),
                other(79001, EquipEffectCategory.NONE)));
        byGrade.put(ItemGrade.RARE, List.of(
                stat(70003, EquipEffectStatType.DEFENSE, EquipEffectValueType.ADD, 12),
                stat(70004, EquipEffectStatType.DAMAGE_REDUCTION, EquipEffectValueType.ADD, 8),
                other(71001, EquipEffectCategory.TRIGGER)));
        return new EquipmentEffectCalculator(Map.of(ITEM, byGrade));
    }

    @Test
    @DisplayName("General은 General 효과만 적용된다")
    void general() {
        assertThat(calculator().effectsOf(ITEM, ItemGrade.GENERAL))
                .isEqualTo(new EquipmentEffectSum(10, 0, 0));
    }

    @Test
    @DisplayName("Super는 General과 Super 효과가 누적되고 같은 스탯은 합산된다")
    void superAccumulates() {
        assertThat(calculator().effectsOf(ITEM, ItemGrade.SUPER))
                .isEqualTo(new EquipmentEffectSum(25, 0, 0));
    }

    @Test
    @DisplayName("Rare는 세 등급이 모두 누적되고, 제외·Trigger·서버 미반영 스탯은 건너뛴다")
    void rareAccumulatesAndSkips() {
        assertThat(calculator().effectsOf(ITEM, ItemGrade.RARE))
                .isEqualTo(new EquipmentEffectSum(25, 0, 12));
    }

    @Test
    @DisplayName("등급 효과가 없는 아이템은 0이다")
    void noEffects() {
        assertThat(calculator().effectsOf(9999, ItemGrade.RARE))
                .isEqualTo(EquipmentEffectSum.ZERO);
    }
}