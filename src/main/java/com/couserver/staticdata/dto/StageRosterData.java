package com.couserver.staticdata.dto;

public record StageRosterData(
        int stageRosterId,
        int stageId,
        String monsterSlot,
        int monsterId,
        double monsterHpScale,
        double monsterDamageScale) {
}
