package com.couserver.inventory;

import com.couserver.inventory.entity.ItemGrade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ItemGradeTest {

    @Test
    @DisplayName("등급은 Common부터 Legendary까지 5단계다")
    void order() {
        assertThat(ItemGrade.values()).containsExactly(
                ItemGrade.COMMON, ItemGrade.UNCOMMON, ItemGrade.RARE, ItemGrade.EPIC, ItemGrade.LEGENDARY);
    }

    @Test
    @DisplayName("다음 등급은 한 단계 위다")
    void next() {
        assertThat(ItemGrade.COMMON.next()).isEqualTo(ItemGrade.UNCOMMON);
        assertThat(ItemGrade.RARE.next()).isEqualTo(ItemGrade.EPIC);
        assertThat(ItemGrade.EPIC.next()).isEqualTo(ItemGrade.LEGENDARY);
    }

    @Test
    @DisplayName("Common~Rare만 합성할 수 있다 (Epic 이상은 임시 차단)")
    void canSynthesize() {
        assertThat(ItemGrade.COMMON.canSynthesize()).isTrue();
        assertThat(ItemGrade.UNCOMMON.canSynthesize()).isTrue();
        assertThat(ItemGrade.RARE.canSynthesize()).isTrue();
        assertThat(ItemGrade.EPIC.canSynthesize()).isFalse();
        assertThat(ItemGrade.LEGENDARY.canSynthesize()).isFalse();
    }

    @Test
    @DisplayName("클라이언트 표기는 PascalCase다")
    void clientName() {
        assertThat(ItemGrade.COMMON.toClientName()).isEqualTo("Common");
        assertThat(ItemGrade.UNCOMMON.toClientName()).isEqualTo("Uncommon");
        assertThat(ItemGrade.LEGENDARY.toClientName()).isEqualTo("Legendary");
    }
}