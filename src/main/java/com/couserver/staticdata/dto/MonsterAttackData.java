package com.couserver.staticdata.dto;

public record MonsterAttackData(
        int monsterAttackId,
        int monsterAttackSetId,
        MonsterAttackType monsterAttackType,
        double monsterAttackCooldown,
        double monsterAttackTriggerRange,
        int monsterAttackDamage,
        int monsterAttackCount,
        double monsterAttackAngle,
        double monsterAttackSpeed,
        double monsterAttackDistance,
        double monsterAttackRadius,
        double monsterAttackSpread,
        double monsterAttackDelay,
        double monsterAttackDuration) {
}
