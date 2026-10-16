package com.couserver.battle;

import com.couserver.battle.service.MinDamageEstimator;
import com.couserver.battle.service.MinDamageEstimator.MonsterSpawn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MinDamageEstimatorTest {
    // 일부러 체력이 높은 것을 앞에 둔다
    private static final List<MonsterSpawn> SPAWNS = List.of(
            new MonsterSpawn(1000, 2),
            new MonsterSpawn(100, 5));

    @Test
    @DisplayName("체력이 낮은 몬스터부터 킬 수만큼 더한다")
    void lowestHpFirst() {
        assertThat(MinDamageEstimator.minDamage(SPAWNS, 3)).isEqualTo(300);               // 100 × 3
        assertThat(MinDamageEstimator.minDamage(SPAWNS, 6)).isEqualTo(5 * 100 + 1000);    // 100 × 5 + 1000 × 1
    }

    @Test
    @DisplayName("킬 수가 0이면 필요한 피해도 0이다")
    void zeroKills() {
        assertThat(MinDamageEstimator.minDamage(SPAWNS, 0)).isZero();
    }

    @Test
    @DisplayName("킬 수가 나온 몬스터 수보다 많으면 나온 몬스터 전부의 체력을 더한다")
    void killsBeyondSpawnsCountsAll() {
        assertThat(MinDamageEstimator.minDamage(SPAWNS, 100)).isEqualTo(5 * 100 + 2 * 1000);
    }
}