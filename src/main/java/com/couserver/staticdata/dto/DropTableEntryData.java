package com.couserver.staticdata.dto;

public record DropTableEntryData(
        int dropTableId,
        int dropGroup,
        DropItemType dropItemType,
        int weight,
        int count) {
}
