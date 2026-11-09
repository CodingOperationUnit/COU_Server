package com.couserver.inventory.dto;

import java.util.List;

public record EquipBestResponse(
        List<EquipmentResponse> equippedItems,     // 새로 장착된 장비
        List<EquipmentResponse> unequippedItems) { // 같은 슬롯에서 해제된 장비
}
