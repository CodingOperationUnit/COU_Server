package com.couserver.battle;

import com.couserver.battle.service.BattleTimePolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BattleTimePolicyTest {

    @Test
    @DisplayName("배속이 없으면 게임 시간은 서버가 잰 시간을 넘지 않는다")
    void realTimeAtDefaultSpeed() {
        assertThat(new BattleTimePolicy(false, 1.0).maxGameSeconds(100)).isEqualTo(100.0);
    }

    @Test
    @DisplayName("배속 한도를 올리면 그만큼 게임 시간을 늘려 본다")
    void speedMultiplierStretchesTime() {
        assertThat(new BattleTimePolicy(false, 2.0).maxGameSeconds(100)).isEqualTo(200.0);
    }

    @Test
    @DisplayName("dev 도구가 켜져 있으면 시간 검증을 건너뛴다")
    void devToolsDisableTimeLimits() {
        assertThat(new BattleTimePolicy(true, 1.0).maxGameSeconds(1)).isInfinite();
    }

    @Test
    @DisplayName("배속 한도가 0 이하면 서버가 기동되지 않는다")
    void nonPositiveMultiplierIsRejected() {
        assertThatThrownBy(() -> new BattleTimePolicy(false, 0))
                .isInstanceOf(IllegalStateException.class);
    }
}