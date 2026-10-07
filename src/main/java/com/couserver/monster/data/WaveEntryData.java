package com.couserver.monster.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WaveEntryData(
        int waveEntryId,
        int waveId,
        float patternStartTime,
        int patternId,
        int monsterId
) {
}
