package com.couserver.battle.dto;

import jakarta.validation.constraints.Min;

// 클라이언트가 집계한 전투 결과. 서버가 스테이지 상한으로 검증한다. 계정 경험치는 서버가 계산하므로 받지 않는다
public record BattleResultRequest(
        boolean victory,
        @Min(0) int seconds,
        @Min(0) int kills,
        @Min(0) int gold,
        @Min(0) int rewardBoxes) {
}
