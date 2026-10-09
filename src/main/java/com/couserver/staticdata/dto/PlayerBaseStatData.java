package com.couserver.staticdata.dto;

public record PlayerBaseStatData(int playerBaseAttack,
                                 int playerBaseHp,
                                 int playerBaseCriticalDamage,
                                 int playerBaseCriticalChance,
                                 int playerBaseSkillDamage,
                                 float playerBaseMoveSpeed,
                                 float playerBaseMaxMoveSpeed,
                                 float playerBaseLootRadius) {

}
