package com.couserver.staticdata.dto;

public record WaveEntryData(
        int waveEntryId,
        int waveId,
        double patternStartTime,
        int patternId,
        int monsterId) {
}
