package com.couserver.staticdata.dto;

public record SpawnPatternData(
        int patternId,
        SpawnEventType eventType,
        int spawnCount,
        double spawnInterval,
        double patternDuration,
        int dropTableId) {
}
