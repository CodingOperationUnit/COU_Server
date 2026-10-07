package com.couserver.monster.table;

import com.couserver.monster.data.MonsterData;
import com.couserver.monster.data.MonsterDataFile;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonsterTable {
    public static final String TABLE_NAME = "monster";
    private static final String FILE_PATH = "data/Monster.json";

    private final ObjectMapper objectMapper;

    private Map<Integer, MonsterData> byId;   // 서버 계산용
    private JsonNode original;                // 클라이언트 전달용 (원본 그대로)

    @PostConstruct
    void load() {
        byte[] raw = TableFileReader.read(FILE_PATH);

        original = objectMapper.readTree(raw);
        MonsterDataFile file = objectMapper.readValue(raw, MonsterDataFile.class);

        byId = file.datas().stream()
                .collect(Collectors.toUnmodifiableMap(MonsterData::monsterId, Function.identity()));

        log.info("[{}] {}개 로드", TABLE_NAME, byId.size());
    }

    public MonsterData get(int monsterId) {
        MonsterData data = byId.get(monsterId);
        if (data == null) {
            throw new IllegalArgumentException("존재하지 않는 monsterId: " + monsterId);
        }
        return data;
    }

    public boolean exists(int monsterId) {
        return byId.containsKey(monsterId);
    }

    public JsonNode original() {
        return original;
    }
}
