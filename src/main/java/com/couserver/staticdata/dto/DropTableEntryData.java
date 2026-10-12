package com.couserver.staticdata.dto;

public record DropTableEntryData(
        MonsterType monsterType,
        int dropGroup,
        DropItemType dropItemType,
        int weight,
        int count) {
}
