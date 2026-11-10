package com.couserver.inventory;

import com.couserver.inventory.entity.ItemGrade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemGradeTest {

    @Test
    @DisplayName("등급은 Common부터 Legendary까지 5단계다")
    void order() {
        assertThat(ItemGrade.values()).containsExactly(
                ItemGrade.COMMON, ItemGrade.UNCOMMON, ItemGrade.RARE, ItemGrade.EPIC, ItemGrade.LEGENDARY);
    }

    @Test
    @DisplayName("Legendary에서는 다음 등급으로 승급할 수 없다")
    void legendaryCannotUpgrade() {
        assertThatThrownBy(ItemGrade.LEGENDARY::next)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Common부터 Epic까지 합성할 수 있고 Legendary는 합성할 수 없다")
    void canSynthesize() {
        assertThat(ItemGrade.COMMON.canSynthesize()).isTrue();
        assertThat(ItemGrade.UNCOMMON.canSynthesize()).isTrue();
        assertThat(ItemGrade.RARE.canSynthesize()).isTrue();
        assertThat(ItemGrade.EPIC.canSynthesize()).isTrue();
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