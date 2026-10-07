package com.couserver.monster.table;

import com.couserver.monster.data.SpawnPatternData;
import com.couserver.monster.data.SpawnPatternDataFile;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpawnPatternTable {
    public static final String TABLE_NAME = "spawn-pattern";
    private static final String FILE_PATH = "data/SpawnPattern.json";

    private final ObjectMapper objectMapper;

    private Map<Integer, SpawnPatternData> byId;
    private JsonNode original;

    @PostConstruct
    void load() {
        byte[] raw = TableFileReader.read(FILE_PATH);

        original = objectMapper.readTree(raw);
        List<SpawnPatternData> datas = objectMapper.readValue(raw, SpawnPatternDataFile.class).datas();

        validate(datas);

        byId = datas.stream()
                .collect(Collectors.toUnmodifiableMap(SpawnPatternData::patternId, Function.identity()));

        log.info("[{}] {}개 로드", TABLE_NAME, byId.size());
    }

    private void validate(List<SpawnPatternData> datas) {
        for (SpawnPatternData p : datas) {
            if (p.spawnCount() < 1) {
                throw new IllegalStateException(String.format(
                        "[%s] patternId %d: spawnCount는 1 이상이어야 합니다. (값: %d)",
                        TABLE_NAME, p.patternId(), p.spawnCount()));
            }
            if (p.patternDuration() < 0f) {
                throw new IllegalStateException(String.format(
                        "[%s] patternId %d: patternDuration은 0 이상이어야 합니다.",
                        TABLE_NAME, p.patternId()));
            }
            // 반복 패턴인데 간격이 0이면 같은 순간에 끝없이 생성될 수 있음
            if (p.isRepeat() && p.spawnInterval() <= 0f) {
                throw new IllegalStateException(String.format(
                        "[%s] patternId %d: 반복 패턴은 spawnInterval이 0보다 커야 합니다.",
                        TABLE_NAME, p.patternId()));
            }
        }
    }

    public SpawnPatternData get(int patternId) {
        SpawnPatternData data = byId.get(patternId);
        if (data == null) {
            throw new IllegalArgumentException("존재하지 않는 patternId: " + patternId);
        }
        return data;
    }

    public boolean exists(int patternId) {
        return byId.containsKey(patternId);
    }

    public JsonNode original() {
        return original;
    }
}
