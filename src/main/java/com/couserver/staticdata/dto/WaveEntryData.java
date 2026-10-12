package com.couserver.staticdata.dto;

public record WaveEntryData(
        int waveEntryId,
        int waveId,
        double patternStartTime,
        double patternDuration,
        double spawnInterval,
        int patternId,
        String monsterSlot) {
}
