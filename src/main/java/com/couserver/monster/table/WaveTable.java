package com.couserver.monster.table;

import com.couserver.monster.data.WaveEntryData;
import com.couserver.monster.data.WaveEntryDataFile;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaveTable {

    public static final String TABLE_NAME = "wave";
    private static final String FILE_PATH = "data/Wave.json";

    private final ObjectMapper objectMapper;
    private final MonsterTable monsterTable;
    private final SpawnPatternTable spawnPatternTable;

    private Map<Integer, List<WaveEntryData>> byWaveId;  // waveId → 시작 시간 순 항목들
    private JsonNode original;

    @PostConstruct
    void load() {
        byte[] raw = TableFileReader.read(FILE_PATH);

        original = objectMapper.readTree(raw);
        List<WaveEntryData> datas = objectMapper.readValue(raw, WaveEntryDataFile.class).datas();

        validate(datas);

        byWaveId = datas.stream()
                .sorted(Comparator.comparing(WaveEntryData::patternStartTime))   // 시간 순 정렬
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(WaveEntryData::waveId, Collectors.toUnmodifiableList()),
                        Map::copyOf));

        log.info("[{}] {}개 로드 (웨이브 {}개)", TABLE_NAME, datas.size(), byWaveId.size());
    }

    private void validate(List<WaveEntryData> datas) {
        Set<Integer> seenIds = new HashSet<>();

        for (WaveEntryData e : datas) {
            // 1) waveEntryId 중복 (byWaveId는 waveId로 묶어서 toUnmodifiableMap의 중복 검사가 없음)
            if (!seenIds.add(e.waveEntryId())) {
                throw new IllegalStateException(String.format(
                        "[%s] waveEntryId %d가 중복됩니다.", TABLE_NAME, e.waveEntryId()));
            }
            // 2) 다른 테이블 참조
            if (!spawnPatternTable.exists(e.patternId())) {
                throw new IllegalStateException(String.format(
                        "[%s] waveEntryId %d의 patternId %d가 SpawnPattern에 없습니다.",
                        TABLE_NAME, e.waveEntryId(), e.patternId()));
            }
            if (!monsterTable.exists(e.monsterId())) {
                throw new IllegalStateException(String.format(
                        "[%s] waveEntryId %d의 monsterId %d가 Monster에 없습니다.",
                        TABLE_NAME, e.waveEntryId(), e.monsterId()));
            }
            // 3) 값 자체
            if (e.patternStartTime() < 0f) {
                throw new IllegalStateException(String.format(
                        "[%s] waveEntryId %d: patternStartTime은 0 이상이어야 합니다.",
                        TABLE_NAME, e.waveEntryId()));
            }
        }
    }

    public List<WaveEntryData> entriesOf(int waveId) {
        return byWaveId.getOrDefault(waveId, List.of());
    }

    public boolean exists(int waveId) {
        return byWaveId.containsKey(waveId);
    }

    public JsonNode original() {
        return original;
    }
}
