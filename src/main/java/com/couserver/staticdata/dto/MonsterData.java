package com.couserver.staticdata.dto;

public record MonsterData(
        int monsterId,
        MonsterType monsterType,
        int monsterFamily,             // 계열 1~5. 보스·상자는 0
        int monsterMaxHealthPoint) {
}
