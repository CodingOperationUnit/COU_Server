package com.couserver.staticdata.dto;

public record StageRosterData(
        int stageRosterId,
        int stageId,
        String monsterSlot,
        int monsterId,
        double monsterHpScale,
        double monsterDamageScale,
        int patternId) {               // SpawnPattern 참조. 0이면 웨이브 행의 patternId
}
