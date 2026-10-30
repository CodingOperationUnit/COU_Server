package com.couserver.battle.service;

import com.couserver.staticdata.dto.SpawnPatternData;
import com.couserver.staticdata.dto.WaveEntryData;

// 웨이브 시각표 계산. 클라이언트 WaveManager와 같은 규칙으로, 웨이브는 처치 여부와 무관하게 시각에 맞춰 나온다
public final class SpawnSchedule {
    private static final double MIN_SPAWN_INTERVAL = 0.1;   // 클라이언트 WaveManager.minRepeatInterval

    private SpawnSchedule() {
    }

    // 전투 시작 후 seconds초까지(그 시각 포함) 웨이브 항목 하나에서 나올 수 있는 최대 스폰 수.
    // 반복 이벤트는 [시작, 시작 + 지속 시간) 동안 간격마다 나오고, 클라이언트가 간격을 float로 누적하므로
    // 나누어떨어질 때 한 번 더 나올 수 있게 floor + 1로 센다. seconds가 무한대면 항목 전체의 스폰 수다
    public static int spawnCountBy(WaveEntryData entry, SpawnPatternData pattern, double seconds) {
        double start = entry.patternStartTime();
        if (seconds < start) {
            return 0;
        }
        if (entry.patternDuration() <= 0) {
            return pattern.spawnCount();
        }
        double interval = Math.max(entry.spawnInterval(), MIN_SPAWN_INTERVAL);
        double span = seconds >= start + entry.patternDuration() ? entry.patternDuration() : seconds - start;
        return ((int) (span / interval) + 1) * pattern.spawnCount();
    }
}