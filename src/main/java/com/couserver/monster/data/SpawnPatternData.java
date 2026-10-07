package com.couserver.monster.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpawnPatternData(
        int patternId,
        SpawnEventType eventType,
        SpawnFormation formation,
        int spawnCount,
        float spawnInterval,
        float patternDuration,
        int dropTableId
) {
    public boolean isRepeat() {
        return patternDuration > 0f;
    }
}
