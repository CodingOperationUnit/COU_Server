package com.couserver.account;

import com.couserver.account.entity.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyTest {
    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final int MAX_STAMINA = 60;
    private static final int RECOVERY_SECONDS = 300;

    @Test
    @DisplayName("gold가 int 최대값을 넘으면 음수가 되지 않고 최대값에서 멈춘다")
    void addGoldCapsAtMax() {
        Currency currency = new Currency(1L, Integer.MAX_VALUE - 10, 0, MAX_STAMINA, T0);
        currency.addGold(100);
        assertThat(currency.getCurrencyGold()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("gem이 int 최대값을 넘으면 최대값에서 멈춘다")
    void addGemCapsAtMax() {
        Currency currency = new Currency(1L, 0, Integer.MAX_VALUE - 10, MAX_STAMINA, T0);
        currency.addGem(100);
        assertThat(currency.getCurrencyGem()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("최대값과 먼 경우에는 그대로 더한다")
    void addGoldNormally() {
        Currency currency = new Currency(1L, 100, 0, MAX_STAMINA, T0);
        currency.addGold(50);
        assertThat(currency.getCurrencyGold()).isEqualTo(150);
    }

    @Test
    @DisplayName("energyAt은 회복을 계산하지만 엔티티는 바꾸지 않는다")
    void energyAtDoesNotChangeEntity() {
        Currency currency = new Currency(1L, 0, 0, 50, T0);

        Currency.EnergyState state = currency.energyAt(T0.plusSeconds(650), MAX_STAMINA, RECOVERY_SECONDS);

        assertThat(state.energy()).isEqualTo(52);
        assertThat(state.updatedAt()).isEqualTo(T0.plusSeconds(600));   // 남은 50초는 다음 회복에 이어서 쓴다
        assertThat(currency.getCurrencyEnergy()).isEqualTo(50);
        assertThat(currency.getCurrencyEnergyUpdatedAt()).isEqualTo(T0);
    }

    @Test
    @DisplayName("최대치까지 회복하면 회복 기준 시각이 현재가 된다")
    void energyAtFullRecovery() {
        Currency currency = new Currency(1L, 0, 0, 58, T0);

        Currency.EnergyState state = currency.energyAt(T0.plusSeconds(3000), MAX_STAMINA, RECOVERY_SECONDS);

        assertThat(state.energy()).isEqualTo(MAX_STAMINA);
        assertThat(state.updatedAt()).isEqualTo(T0.plusSeconds(3000));
    }

    @Test
    @DisplayName("recoverEnergy는 energyAt과 같은 결과를 엔티티에 반영한다")
    void recoverEnergyMatchesEnergyAt() {
        Currency currency = new Currency(1L, 0, 0, 50, T0);
        Instant now = T0.plusSeconds(650);
        Currency.EnergyState expected = currency.energyAt(now, MAX_STAMINA, RECOVERY_SECONDS);

        currency.recoverEnergy(now, MAX_STAMINA, RECOVERY_SECONDS);

        assertThat(currency.getCurrencyEnergy()).isEqualTo(expected.energy());
        assertThat(currency.getCurrencyEnergyUpdatedAt()).isEqualTo(expected.updatedAt());
    }
}