package com.couserver.staticdata.dto;

public record StageRosterData(
        int stageRosterId,
        int stageId,
        String monsterSlot,
        int monsterId,
        double monsterHpScale,
        double monsterDamageScale,
        int patternId,                 // SpawnPattern 참조. 0이면 웨이브 행의 patternId
        DropItemType expGemType) {     // 처치 시 떨어뜨리는 경험치 잼 EXPGEM1~4. NONE이면 없음
}
