package com.couserver.battle.entity;

// 전투 세션 상태. 진행 중인 세션에만 보상을 지급한다
public enum BattleStatus {
    IN_PROGRESS,
    COMPLETED,
    EXPIRED     // 결과를 받기 전에 새 전투에 입장함
}
