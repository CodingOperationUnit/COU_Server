package com.couserver.staticdata;

import com.couserver.inventory.entity.ItemGrade;
import com.couserver.staticdata.dto.*;
import com.couserver.staticdata.service.StaticDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EquipEffectValidationTest {

    private static EquipEffectData stat(long id, EquipEffectStatType statType, EquipEffectValueType valueType, double value) {
        return new EquipEffectData(id, "테스트", EquipEffectCategory.STAT, statType, valueType, value,
                EquipEffectTriggerType.NONE, 0, 0, EquipEffectActionType.NONE, 0);
    }

    private static EquipEffectData of(long id, EquipEffectCategory category,
                                      EquipEffectTriggerType trigger, EquipEffectActionType action) {
        return new EquipEffectData(id, "테스트", category, EquipEffectStatType.NONE, EquipEffectValueType.NONE, 0,
                trigger, 0, 0, action, 0);
    }

    @Test
    @DisplayName("올바른 Stat형 효과는 통과한다")
    void validStat() {
        assertThatCode(() -> StaticDataService.validateEquipEffect(
                stat(70001, EquipEffectStatType.HP, EquipEffectValueType.PERCENT, 10))).doesNotThrowAnyException();
        assertThatCode(() -> StaticDataService.validateEquipEffect(
                stat(70002, EquipEffectStatType.DEFENSE, EquipEffectValueType.ADD, 12))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Stat형 조합이 규칙과 다르면 실패한다 (HP는 Percent)")
    void wrongValueType() {
        assertThatThrownBy(() -> StaticDataService.validateEquipEffect(
                stat(70001, EquipEffectStatType.HP, EquipEffectValueType.ADD, 10)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("valueType");
    }

    @Test
    @DisplayName("HP·ATTACK·DEFENSE 수치가 정수가 아니면 실패한다")
    void nonIntegerServerStat() {
        assertThatThrownBy(() -> StaticDataService.validateEquipEffect(
                stat(70002, EquipEffectStatType.DEFENSE, EquipEffectValueType.ADD, 2.5)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("정수");
    }

    @Test
    @DisplayName("서버가 계산하지 않는 스탯은 소수여도 통과한다")
    void nonIntegerClientStat() {
        assertThatCode(() -> StaticDataService.validateEquipEffect(
                stat(70003, EquipEffectStatType.MOVE_SPEED, EquipEffectValueType.PERCENT, 2.5))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("제외 대상이 아닌데 카테고리가 None이면 실패한다")
    void noneCategoryNotExcluded() {
        assertThatThrownBy(() -> StaticDataService.validateEquipEffect(
                of(70004, EquipEffectCategory.NONE, EquipEffectTriggerType.NONE, EquipEffectActionType.NONE)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("None");
    }

    @Test
    @DisplayName("제외 대상(79xxx)은 카테고리가 None이어도 통과한다")
    void excludedSkipsChecks() {
        assertThatCode(() -> StaticDataService.validateEquipEffect(
                of(79001, EquipEffectCategory.NONE, EquipEffectTriggerType.NONE, EquipEffectActionType.NONE))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Trigger형에 동작이 없으면 실패한다")
    void triggerWithoutAction() {
        assertThatThrownBy(() -> StaticDataService.validateEquipEffect(
                of(71001, EquipEffectCategory.TRIGGER, EquipEffectTriggerType.ONDAMAGED, EquipEffectActionType.NONE)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("actionType");
    }

    @Test
    @DisplayName("ItemGradeEffect의 itemId·effectId가 없으면 실패한다")
    void itemGradeEffectForeignKeys() {
        Map<Long, EquipEffectData> effects = Map.of(70001L,
                stat(70001, EquipEffectStatType.HP, EquipEffectValueType.PERCENT, 10));
        Set<Long> items = Set.of(1003L);

        assertThatCode(() -> StaticDataService.validateItemGradeEffect(
                new ItemGradeEffectData(1, 1003, ItemGrade.GENERAL, 70001), items, effects)).doesNotThrowAnyException();
        assertThatThrownBy(() -> StaticDataService.validateItemGradeEffect(
                new ItemGradeEffectData(2, 9999, ItemGrade.GENERAL, 70001), items, effects))
                .hasMessageContaining("Item");
        assertThatThrownBy(() -> StaticDataService.validateItemGradeEffect(
                new ItemGradeEffectData(3, 1003, ItemGrade.GENERAL, 70999), items, effects))
                .hasMessageContaining("EquipEffect");
    }
}