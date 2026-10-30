package com.couserver.battle.dto;

import jakarta.validation.constraints.Min;

import java.util.Map;

// 클라이언트가 집계한 전투 결과. 서버가 스테이지 상한으로 검증한다. 계정 경험치는 서버가 계산하므로 받지 않는다
// 업적 집계(skillKills, skillUpgrades, pickups, luckTrains)는 보내지 않으면 null / 0
public record BattleResultRequest(
        boolean victory,
        @Min(0) int seconds,
        @Min(0) int kills,
        @Min(0) int gold,
        @Min(0) int rewardBoxes,
        @Min(0) int quality,                  // 프로젝트 퀄리티. 승리일 때만 BattleConst.qualityMax까지 인정한다
        Map<Integer, Integer> skillKills,     // skillId → 처치 수
        Map<Integer, Integer> skillUpgrades,  // skillId → 레벨업 횟수 (레벨업 선택만, 행운열차 제외)
        Map<String, Integer> pickups,         // DropItemType 이름 → 획득 수
        @Min(0) int luckTrains) {             // 행운열차 완주 횟수
}