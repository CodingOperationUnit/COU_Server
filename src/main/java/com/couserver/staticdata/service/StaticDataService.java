package com.couserver.staticdata.service;

import com.couserver.staticdata.dto.AccountConstData;
import com.couserver.staticdata.dto.DropItemData;
import com.couserver.staticdata.dto.DropItemType;
import com.couserver.staticdata.dto.DropTableEntryData;
import com.couserver.staticdata.dto.ItemData;
import com.couserver.staticdata.dto.MonsterData;
import com.couserver.staticdata.dto.SpawnPatternData;
import com.couserver.staticdata.dto.StageData;
import com.couserver.staticdata.dto.WaveEntryData;
import lombok.Getter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// 시작할 때 data/의 정적 데이터를 읽고 검증해 메모리에 둔다. 원본 JSON은 클라이언트에 그대로 보내기 위해 함께 둔다.
@Getter
@Service
public class StaticDataService {
    private static final List<String> TABLE_NAMES = List.of(
            "AccountConst", "Stage", "Wave", "SpawnPattern", "Monster", "DropTable", "DropItem", "Item");

    private final String version;
    private final Map<String, JsonNode> tables;

    private final AccountConstData accountConst;
    private final Map<Integer, StageData> stages;
    private final Map<Integer, List<WaveEntryData>> waves;              // key: waveId
    private final Map<Integer, SpawnPatternData> spawnPatterns;
    private final Map<Integer, MonsterData> monsters;
    private final Map<Integer, List<DropTableEntryData>> dropTables;    // key: dropTableId
    private final Map<DropItemType, DropItemData> dropItems;            // key: dropItemType
    private final Map<Long, ItemData> items;

    public StaticDataService(ObjectMapper objectMapper) throws IOException {
        version = new ClassPathResource("data/version.txt").getContentAsString(StandardCharsets.UTF_8).trim();

        Map<String, JsonNode> loaded = new LinkedHashMap<>();
        for (String name : TABLE_NAMES) {
            try (InputStream in = new ClassPathResource("data/" + name + ".json").getInputStream()) {
                loaded.put(name, objectMapper.readTree(in));
            }
        }
        tables = Collections.unmodifiableMap(loaded);

        // 시트 값(Armor)을 대문자 enum(ARMOR)으로 읽고, record 필드에 해당하는 열이 없으면 실패한다
        ObjectMapper rowMapper = objectMapper.rebuild()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                .build();

        List<AccountConstData> accountConstRows = rows(rowMapper, "AccountConst", AccountConstData.class);
        require(accountConstRows.size() == 1, "AccountConst 데이터는 행이 하나여야 합니다.");
        accountConst = accountConstRows.getFirst();

        List<StageData> stageRows = rows(rowMapper, "Stage", StageData.class);
        requireIds("Stage", stageRows, StageData::stageId);
        stages = stageRows.stream()
                .collect(Collectors.toUnmodifiableMap(StageData::stageId, Function.identity()));

        List<WaveEntryData> waveRows = rows(rowMapper, "Wave", WaveEntryData.class);
        requireIds("Wave", waveRows, WaveEntryData::waveEntryId);
        waves = Map.copyOf(waveRows.stream()
                .collect(Collectors.groupingBy(WaveEntryData::waveId, Collectors.toUnmodifiableList())));

        List<SpawnPatternData> spawnPatternRows = rows(rowMapper, "SpawnPattern", SpawnPatternData.class);
        requireIds("SpawnPattern", spawnPatternRows, SpawnPatternData::patternId);
        for (SpawnPatternData row : spawnPatternRows) {
            require(row.spawnCount() >= 1, "SpawnPattern " + row.patternId() + "의 spawnCount는 1 이상이어야 합니다.");
        }
        spawnPatterns = spawnPatternRows.stream()
                .collect(Collectors.toUnmodifiableMap(SpawnPatternData::patternId, Function.identity()));

        List<MonsterData> monsterRows = rows(rowMapper, "Monster", MonsterData.class);
        requireIds("Monster", monsterRows, MonsterData::monsterId);
        monsters = monsterRows.stream()
                .collect(Collectors.toUnmodifiableMap(MonsterData::monsterId, Function.identity()));

        List<DropTableEntryData> dropTableRows = rows(rowMapper, "DropTable", DropTableEntryData.class);
        for (DropTableEntryData row : dropTableRows) {
            require(row.dropTableId() > 0, "DropTable ID가 올바르지 않습니다: " + row.dropTableId());
            require(row.dropGroup() >= 0, "DropTable " + row.dropTableId() + "의 dropGroup은 0 이상이어야 합니다.");
            require(row.weight() >= 1, "DropTable " + row.dropTableId() + "의 weight는 1 이상이어야 합니다.");
            require(row.count() >= 1, "DropTable " + row.dropTableId() + "의 count는 1 이상이어야 합니다.");
        }
        dropTables = Map.copyOf(dropTableRows.stream()
                .collect(Collectors.groupingBy(DropTableEntryData::dropTableId, Collectors.toUnmodifiableList())));

        List<DropItemData> dropItemRows = rows(rowMapper, "DropItem", DropItemData.class);
        requireIds("DropItem", dropItemRows, DropItemData::dropItemId);
        for (DropItemData row : dropItemRows) {
            require(row.dropItemType() != DropItemType.NONE, "DropItem " + row.dropItemId() + "의 dropItemType은 None일 수 없습니다.");
        }
        dropItems = dropItemRows.stream()
                .collect(Collectors.toUnmodifiableMap(DropItemData::dropItemType, Function.identity()));

        List<ItemData> itemRows = rows(rowMapper, "Item", ItemData.class);
        requireIds("Item", itemRows, ItemData::itemId);
        items = itemRows.stream()
                .collect(Collectors.toUnmodifiableMap(ItemData::itemId, Function.identity()));

        for (StageData row : stageRows) {
            require(waves.containsKey(row.waveId()),
                    "Stage " + row.stageId() + "의 waveId " + row.waveId() + "을(를) Wave에서 찾을 수 없습니다.");
        }
        for (WaveEntryData row : waveRows) {
            require(spawnPatterns.containsKey(row.patternId()),
                    "Wave " + row.waveEntryId() + "의 patternId " + row.patternId() + "을(를) SpawnPattern에서 찾을 수 없습니다.");
            require(monsters.containsKey(row.monsterId()),
                    "Wave " + row.waveEntryId() + "의 monsterId " + row.monsterId() + "을(를) Monster에서 찾을 수 없습니다.");
        }
        for (SpawnPatternData row : spawnPatternRows) {
            require(row.dropTableId() == 0 || dropTables.containsKey(row.dropTableId()),
                    "SpawnPattern " + row.patternId() + "의 dropTableId " + row.dropTableId() + "을(를) DropTable에서 찾을 수 없습니다.");
        }
    }

    private <T> List<T> rows(ObjectMapper rowMapper, String name, Class<T> type) {
        JsonNode datas = tables.get(name).get("datas");
        require(datas != null && datas.isArray() && !datas.isEmpty(), name + " 데이터 목록이 비어 있습니다.");
        return rowMapper.readerForListOf(type).readValue(datas);
    }

    // 행마다 ID가 1 이상이고 겹치지 않는지 확인한다
    private static <T> void requireIds(String name, List<T> rows, Function<T, ? extends Number> id) {
        Set<Number> seen = new HashSet<>();
        for (T row : rows) {
            Number value = id.apply(row);
            require(value.longValue() > 0, name + " ID가 올바르지 않습니다: " + value);
            require(seen.add(value), "중복된 " + name + " ID: " + value);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
