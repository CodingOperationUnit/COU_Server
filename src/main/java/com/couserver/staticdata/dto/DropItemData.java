package com.couserver.staticdata.dto;

public record DropItemData(
        int dropItemId,
        DropItemType dropItemType,
        int value) {
}
