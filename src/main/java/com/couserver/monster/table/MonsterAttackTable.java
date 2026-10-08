package com.couserver.monster.table;

import com.couserver.monster.data.MonsterAttackData;
import com.couserver.monster.data.MonsterAttackDataFile;
import com.couserver.monster.data.MonsterAttackType;
import com.couserver.monster.data.MonsterType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonsterAttackTable {

    public static final String TABLE_NAME = "monster-attack";
    private static final String FILE_PATH = "data/MonsterAttack.json";

    private final ObjectMapper objectMapper;
    private final MonsterTable monsterTable;          // ← 다른 테이블 주입

    private Map<Integer, List<MonsterAttackData>> byMonsterId;   // 몬스터 monsterId → 그 몬스터의 패턴들
    private JsonNode original;

    @PostConstruct
    void load() {
        byte[] raw = TableFileReader.read(FILE_PATH);

        original = objectMapper.readTree(raw);
        List<MonsterAttackData> datas = objectMapper.readValue(raw, MonsterAttackDataFile.class).datas();

        validate(datas);

        byMonsterId = datas.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(MonsterAttackData::monsterId, Collectors.toUnmodifiableList()),
                        Map::copyOf));                   // 바깥 Map도 수정 불가로

        log.info("[{}] {}개 로드 (몬스터 {}종)", TABLE_NAME, datas.size(), byMonsterId.size());
    }

    // 검증: monsterId가 Monster에 있어야 하고, 상자는 공격 불가, 보스가 아닌 몬스터는 Melee 불가
    private void validate(List<MonsterAttackData> datas) {
        for (MonsterAttackData d : datas) {
            if (!monsterTable.exists(d.monsterId())) {
                throw new IllegalStateException(String.format(
                        "[%s] monsterAttackId %d의 monsterId %d가 Monster에 없습니다.",
                        TABLE_NAME, d.monsterAttackId(), d.monsterId()));
            }
            MonsterType type = monsterTable.get(d.monsterId()).monsterType();
            if (type == MonsterType.Box) {
                throw new IllegalStateException(String.format(
                        "[%s] monsterAttackId %d: 상자(monsterId %d)는 공격을 가질 수 없습니다.",
                        TABLE_NAME, d.monsterAttackId(), d.monsterId()));
            }
            if (type != MonsterType.Boss && d.monsterAttackType() == MonsterAttackType.Melee) {
                throw new IllegalStateException(String.format(
                        "[%s] monsterAttackId %d: 보스가 아닌 몬스터(monsterId %d)는 Melee를 쓸 수 없습니다.",
                        TABLE_NAME, d.monsterAttackId(), d.monsterId()));
            }
        }
    }

    public List<MonsterAttackData> attacksOf(int monsterId) {
        return byMonsterId.getOrDefault(monsterId, List.of());
    }

    public JsonNode original() {

        return original;
    }
}
