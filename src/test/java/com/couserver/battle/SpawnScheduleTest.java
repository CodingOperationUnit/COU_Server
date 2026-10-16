package com.couserver.battle;

import com.couserver.battle.service.SpawnSchedule;
import com.couserver.staticdata.dto.SpawnPatternData;
import com.couserver.staticdata.dto.WaveEntryData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpawnScheduleTest {
    private static final SpawnPatternData THREE_PER_SPAWN = new SpawnPatternData(1, 3);

    @Test
    @DisplayName("시작 시각 전에는 아무것도 나오지 않는다")
    void nothingBeforeStart() {
        assertThat(SpawnSchedule.spawnCountBy(entry(10, 0, 0), THREE_PER_SPAWN, 9.9)).isZero();
        assertThat(SpawnSchedule.spawnCountBy(entry(10, 60, 2), THREE_PER_SPAWN, 9.9)).isZero();
    }

    @Test
    @DisplayName("한 번만 나오는 이벤트는 시작 시각부터 한 번 나온다")
    void singleEvent() {
        assertThat(SpawnSchedule.spawnCountBy(entry(10, 0, 0), THREE_PER_SPAWN, 10)).isEqualTo(3);
        assertThat(SpawnSchedule.spawnCountBy(entry(10, 0, 0), THREE_PER_SPAWN, 500)).isEqualTo(3);
    }

    @Test
    @DisplayName("반복 이벤트는 시각이 지날수록 간격마다 늘어난다")
    void repeatingEventGrowsWithTime() {
        WaveEntryData repeating = entry(0, 10, 2);

        assertThat(SpawnSchedule.spawnCountBy(repeating, THREE_PER_SPAWN, 0)).isEqualTo(3);     // 0초에 한 번
        assertThat(SpawnSchedule.spawnCountBy(repeating, THREE_PER_SPAWN, 3.9)).isEqualTo(6);   // 0, 2초
        assertThat(SpawnSchedule.spawnCountBy(repeating, THREE_PER_SPAWN, 4)).isEqualTo(9);     // 0, 2, 4초
    }

    @Test
    @DisplayName("끝난 뒤나 시간 제한이 없을 때는 항목 전체의 스폰 수와 같다")
    void fullSpanCountsEverything() {
        WaveEntryData repeating = entry(0, 10, 2);
        int full = ((int) (10 / 2.0) + 1) * 3;   // 나누어떨어질 때 한 번 더 나오도록 floor + 1

        assertThat(SpawnSchedule.spawnCountBy(repeating, THREE_PER_SPAWN, 10)).isEqualTo(full);
        assertThat(SpawnSchedule.spawnCountBy(repeating, THREE_PER_SPAWN, 1000)).isEqualTo(full);
        assertThat(SpawnSchedule.spawnCountBy(repeating, THREE_PER_SPAWN, Double.POSITIVE_INFINITY)).isEqualTo(full);
    }

    @Test
    @DisplayName("너무 짧은 간격은 클라이언트처럼 0.1초로 본다")
    void tooShortIntervalIsFloored() {
        int expected = ((int) (1.0 / 0.1) + 1) * 3;

        assertThat(SpawnSchedule.spawnCountBy(entry(0, 1, 0.01), THREE_PER_SPAWN, Double.POSITIVE_INFINITY))
                .isEqualTo(expected);
    }

    private static WaveEntryData entry(double start, double duration, double interval) {
        return new WaveEntryData(1, 1, start, duration, interval, 1, "SLOT");
    }
}