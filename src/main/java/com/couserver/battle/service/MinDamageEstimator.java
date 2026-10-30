package com.couserver.battle.service;

import com.couserver.staticdata.dto.MonsterData;
import com.couserver.staticdata.dto.MonsterType;
import com.couserver.staticdata.dto.SpawnPatternData;
import com.couserver.staticdata.dto.StageData;
import com.couserver.staticdata.dto.StageRosterData;
import com.couserver.staticdata.dto.WaveEntryData;
import com.couserver.staticdata.service.StaticDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

// 인정한 킬 수를 채우려면 최소한 얼마의 피해가 필요한지 추정한다 (하한).
// 그 시각까지 나온 몬스터 중 체력이 낮은 것부터 킬 수만큼 더한다. 정상 플레이어는 이보다 적게 줄 수 없다.
// 전투력 대비 효율이 유난히 높은 계정을 찾는 통계용이고, 확정 근거는 아니다
@Component
@RequiredArgsConstructor
public class MinDamageEstimator {
    private final StaticDataService staticDataService;

    // 같은 체력의 몬스터가 count마리 나온다
    public record MonsterSpawn(long hp, int count) {
    }

    // seconds초까지 나온 몬스터로 kills마리를 잡는 데 필요한 최소 피해
    public long estimate(int stageId, double seconds, int kills) {
        StageData stage = staticDataService.getStages().get(stageId);
        Map<String, StageRosterData> roster = staticDataService.getStageRosters().get(stageId);

        List<MonsterSpawn> spawns = new ArrayList<>();
        for (WaveEntryData entry : staticDataService.getWaves().get(stage.waveId())) {
            StageRosterData rosterRow = roster.get(entry.monsterSlot());
            MonsterData monster = staticDataService.getMonsters().get(rosterRow.monsterId());
            if (monster.monsterType() == MonsterType.BOX) {
                continue;   // 상자는 킬 수에 들어가지 않는다
            }
            int patternId = rosterRow.patternId() > 0 ? rosterRow.patternId() : entry.patternId();
            SpawnPatternData pattern = staticDataService.getSpawnPatterns().get(patternId);
            int count = SpawnSchedule.spawnCountBy(entry, pattern, seconds);
            if (count > 0) {
                // 하한이므로 소수점은 내린다
                long hp = Math.max(1L, (long) Math.floor(monster.monsterMaxHealthPoint() * rosterRow.monsterHpScale()));
                spawns.add(new MonsterSpawn(hp, count));
            }
        }
        return minDamage(spawns, kills);
    }

    // 체력이 낮은 것부터 kills마리를 더한다. 킬 수가 나온 몬스터 수보다 많으면 나온 전부를 더한다
    public static long minDamage(List<MonsterSpawn> spawns, int kills) {
        List<MonsterSpawn> byHp = spawns.stream()
                .sorted(Comparator.comparingLong(MonsterSpawn::hp))
                .toList();
        long total = 0;
        int remaining = kills;
        for (MonsterSpawn spawn : byHp) {
            if (remaining <= 0) {
                break;
            }
            int take = Math.min(remaining, spawn.count());
            total += (long) take * spawn.hp();
            remaining -= take;
        }
        return total;
    }
}