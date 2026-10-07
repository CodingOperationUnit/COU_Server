package com.couserver.monster.table;

import com.couserver.monster.data.BossAttackData;
import com.couserver.monster.data.BossAttackDataFile;
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
public class BossAttackTable {

    public static final String TABLE_NAME = "boss-attack";
    private static final String FILE_PATH = "data/BossAttack.json";

    private final ObjectMapper objectMapper;
    private final MonsterTable monsterTable;          // ← 다른 테이블 주입

    private Map<Integer, List<BossAttackData>> byMonsterId;   // 보스 monsterId → 그 보스의 패턴들
    private JsonNode original;

    @PostConstruct
    void load() {
        byte[] raw = TableFileReader.read(FILE_PATH);

        original = objectMapper.readTree(raw);
        List<BossAttackData> datas = objectMapper.readValue(raw, BossAttackDataFile.class).datas();

        validate(datas);

        byMonsterId = datas.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(BossAttackData::monsterId, Collectors.toUnmodifiableList()),
                        Map::copyOf));                   // 바깥 Map도 수정 불가로

        log.info("[{}] {}개 로드 (보스 {}종)", TABLE_NAME, datas.size(), byMonsterId.size());
    }

    // 관계 정리 문서: BossAttack은 monsterType이 Boss인 몬스터만 참조한다
    private void validate(List<BossAttackData> datas) {
        for (BossAttackData d : datas) {
            if (!monsterTable.exists(d.monsterId())) {
                throw new IllegalStateException(String.format(
                        "[%s] bossAttackId %d의 monsterId %d가 Monster에 없습니다.",
                        TABLE_NAME, d.bossAttackId(), d.monsterId()));
            }
            if (monsterTable.get(d.monsterId()).monsterType() != MonsterType.Boss) {
                throw new IllegalStateException(String.format(
                        "[%s] bossAttackId %d의 monsterId %d는 보스가 아닙니다.",
                        TABLE_NAME, d.bossAttackId(), d.monsterId()));
            }
        }
    }

    public List<BossAttackData> attacksOf(int bossMonsterId) {
        return byMonsterId.getOrDefault(bossMonsterId, List.of());
    }

    public JsonNode original() {
        return original;
    }
}
