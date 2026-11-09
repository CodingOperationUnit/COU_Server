package com.couserver.staticdata.dto;

public record DropTableEntryData(
        String monsterSlot,
        int dropGroup,
        DropItemType dropItemType,
        int weight,
        int count) {
}
