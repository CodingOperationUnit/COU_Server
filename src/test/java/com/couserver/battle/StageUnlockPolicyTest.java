package com.couserver.battle;

import com.couserver.battle.service.StageUnlockPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class StageUnlockPolicyTest {
    // 4번이 비어 있는 스테이지 목록
    private static final Set<Integer> STAGES = Set.of(1, 2, 3, 5);

    @Test
    @DisplayName("클리어 기록이 없으면 첫 스테이지만 열려 있다")
    void onlyFirstStageWithoutClear() {
        assertThat(StageUnlockPolicy.isUnlocked(STAGES, 1, null)).isTrue();
        assertThat(StageUnlockPolicy.isUnlocked(STAGES, 2, null)).isFalse();
    }

    @Test
    @DisplayName("클리어한 스테이지의 바로 다음 스테이지까지 열린다")
    void nextStageAfterClear() {
        assertThat(StageUnlockPolicy.isUnlocked(STAGES, 2, 1)).isTrue();
        assertThat(StageUnlockPolicy.isUnlocked(STAGES, 3, 1)).isFalse();
    }

    @Test
    @DisplayName("스테이지 ID가 비어 있어도 다음 스테이지가 열린다")
    void skipsMissingStageId() {
        assertThat(StageUnlockPolicy.isUnlocked(STAGES, 5, 3)).isTrue();
    }

    @Test
    @DisplayName("정적 데이터에 없는 스테이지는 열리지 않는다")
    void unknownStageIsLocked() {
        assertThat(StageUnlockPolicy.isUnlocked(STAGES, 4, 999)).isFalse();
        assertThat(StageUnlockPolicy.isUnlocked(STAGES, 6, 5)).isFalse();
    }

    @Test
    @DisplayName("이미 클리어한 이전 스테이지는 다시 고를 수 있다")
    void clearedStageIsStillUnlocked() {
        assertThat(StageUnlockPolicy.isUnlocked(STAGES, 1, 3)).isTrue();
    }
}