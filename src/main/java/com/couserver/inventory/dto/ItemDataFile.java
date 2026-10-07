package com.couserver.inventory.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

public record ItemDataFile(List<ItemData> datas) {
}
