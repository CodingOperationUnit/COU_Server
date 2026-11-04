package com.couserver.inventory.dto;


import java.util.List;

public record SynthesizeAllResponse(
        List<EquipmentResponse> synthesizedItems,   // 등급이 오른 장비 (최종 상태, 장비마다 1번)
        List<Long> consumedInventoryIds) {          // 소모된 재료 전부
}
