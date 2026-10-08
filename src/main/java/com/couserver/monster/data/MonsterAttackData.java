package com.couserver.monster.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MonsterAttackData(
        int monsterAttackId,
        int monsterId,
        MonsterAttackType monsterAttackType,
        float monsterAttackCooldown,
        float monsterAttackTriggerRange,
        int monsterAttackDamage,
        int monsterAttackCount,
        float monsterAttackAngle,
        float monsterAttackDuration
) {
}
