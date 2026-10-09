package com.couserver.staticdata.dto;

public record MonsterAttackData(
        int monsterAttackId,
        int monsterId,
        MonsterAttackType monsterAttackType) {
}
