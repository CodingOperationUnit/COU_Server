package com.couserver.monster.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MonsterData(
        int monsterId,
        String monsterName,
        MonsterType monsterType,
        int monsterMaxHealthPoint,
        int monsterExp,
        float monsterMoveSpeed,
        int monsterAttackPoint,
        String monsterAsset
) {
}
