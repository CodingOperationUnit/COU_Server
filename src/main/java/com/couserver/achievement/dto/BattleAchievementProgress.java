package com.couserver.achievement.dto;

import com.couserver.staticdata.dto.DropItemType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

// 전투 한 판에서 서버가 검증을 끝낸 업적 진행값
@Getter
@RequiredArgsConstructor
public class BattleAchievementProgress {
    private final Map<Integer, Integer> skillKills;
    private final Map<Integer, Integer> skillUpgrades;
    private final Map<DropItemType, Integer> pickups;
    private final long battleExp;
    private final long battleGold;   // 검증된 전투 골드 (드롭 + 행운열차)
    private final int stageClears;   // 승리면 1
    private final int luckTrains;    // 행운열차 완주 횟수

    public int totalSkillUpgrades() {
        return skillUpgrades.values().stream().mapToInt(Integer::intValue).sum();
    }
}