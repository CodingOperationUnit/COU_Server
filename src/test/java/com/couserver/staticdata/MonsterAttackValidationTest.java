package com.couserver.staticdata;

import com.couserver.staticdata.dto.MonsterAttackData;
import com.couserver.staticdata.dto.MonsterAttackType;
import com.couserver.staticdata.service.StaticDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.couserver.staticdata.dto.MonsterAttackType.*;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MonsterAttackValidationTest {

    // 공격 세트 13001의 1번 공격. 쿨타임·시작 거리·피해는 모든 타입이 쓰는 값으로 고정한다
    private static MonsterAttackData row(MonsterAttackType type, int count, double angle, double speed, double distance,
                                         double radius, double spread, double delay, double duration) {
        return new MonsterAttackData(130011, 13001, type, 3, 7, 600,
                count, angle, speed, distance, radius, spread, delay, duration);
    }

    private static MonsterAttackData withIds(int monsterAttackId, int monsterAttackSetId) {
        return new MonsterAttackData(monsterAttackId, monsterAttackSetId, RANGED, 3, 7, 600, 1, 0, 6, 0, 0, 0, 0, 4);
    }

    @Test
    @DisplayName("타입별로 쓰는 열만 값이 있는 행은 통과한다")
    void validRowsPass() {
        assertThatCode(() -> StaticDataService.validateMonsterAttack(
                row(MELEE, 0, 0, 12, 6, 0.8, 0, 0, 0))).doesNotThrowAnyException();
        assertThatCode(() -> StaticDataService.validateMonsterAttack(
                row(RANGED, 5, 60, 6, 0, 0, 0, 0, 4))).doesNotThrowAnyException();
        assertThatCode(() -> StaticDataService.validateMonsterAttack(
                row(AREA, 3, 0, 0, 0, 1.5, 3, 0.8, 0))).doesNotThrowAnyException();
        assertThatCode(() -> StaticDataService.validateMonsterAttack(
                row(TRAP, 1, 0, 0, 0, 1.5, 2.5, 0, 5))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("타입이 쓰는 열이 0이면 실패한다")
    void usedColumnZeroFails() {
        assertThatThrownBy(() -> StaticDataService.validateMonsterAttack(
                row(MELEE, 0, 0, 0, 6, 0.8, 0, 0, 0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("monsterAttackSpeed");
        assertThatThrownBy(() -> StaticDataService.validateMonsterAttack(
                row(RANGED, 5, 60, 6, 0, 0, 0, 0, 0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("monsterAttackDuration");
        assertThatThrownBy(() -> StaticDataService.validateMonsterAttack(
                row(AREA, 3, 0, 0, 0, 1.5, 3, 0, 0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("monsterAttackDelay");
        assertThatThrownBy(() -> StaticDataService.validateMonsterAttack(
                row(TRAP, 0, 0, 0, 0, 1.5, 2.5, 0, 5)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("monsterAttackCount");
    }

    @Test
    @DisplayName("타입이 쓰지 않는 열이 0이 아니면 실패한다")
    void unusedColumnNonZeroFails() {
        assertThatThrownBy(() -> StaticDataService.validateMonsterAttack(
                row(MELEE, 1, 0, 12, 6, 0.8, 0, 0, 0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("monsterAttackCount");
        assertThatThrownBy(() -> StaticDataService.validateMonsterAttack(
                row(RANGED, 5, 60, 6, 0, 1.5, 0, 0, 4)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("monsterAttackRadius");
        assertThatThrownBy(() -> StaticDataService.validateMonsterAttack(
                row(AREA, 3, 0, 0, 0, 1.5, 3, 0.8, 5)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("monsterAttackDuration");
        assertThatThrownBy(() -> StaticDataService.validateMonsterAttack(
                row(TRAP, 1, 30, 0, 0, 1.5, 2.5, 0, 5)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("monsterAttackAngle");
    }

    @Test
    @DisplayName("Ranged 1발은 각도 0을 허용한다")
    void singleRangedWithoutAnglePasses() {
        assertThatCode(() -> StaticDataService.validateMonsterAttack(
                row(RANGED, 1, 0, 5, 0, 0, 0, 0, 3))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Ranged 2발 이상인데 각도가 0이면 실패한다")
    void multiRangedWithoutAngleFails() {
        assertThatThrownBy(() -> StaticDataService.validateMonsterAttack(
                row(RANGED, 3, 0, 6, 0, 0, 0, 0, 4)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("monsterAttackAngle");
    }

    @Test
    @DisplayName("monsterAttackId가 세트 ID × 10 + 순번(1~9)이 아니면 실패한다")
    void idMustMatchSet() {
        assertThatThrownBy(() -> StaticDataService.validateMonsterAttack(withIds(130011, 13002)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("monsterAttackSetId");
        assertThatThrownBy(() -> StaticDataService.validateMonsterAttack(withIds(130010, 13001)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("monsterAttackSetId");
    }
}
