package com.couserver.monster.dto;

import com.couserver.monster.entity.Monster;

public record MonsterResponse(
        int monsterId,
        String monsterName,
        String monsterType,
        int monsterMaxHealthPoint,
        int monsterAttackPoint,
        int monsterDefensePoint,
        float monsterMoveSpeed,
        int monsterExp,
        String monsterAsset
        ) {
    public static MonsterResponse from(Monster monster) {
        return new MonsterResponse(
                monster.getMonsterId(),
                monster.getMonsterName(),
                monster.getMonsterType().name(),
                monster.getMonsterMaxHealthPoint(),
                monster.getMonsterAttackPoint(),
                monster.getMonsterDefensePoint(),
                monster.getMonsterMoveSpeed(),
                monster.getMonsterExp(),
                monster.getMonsterAsset()
        );
    }
}
