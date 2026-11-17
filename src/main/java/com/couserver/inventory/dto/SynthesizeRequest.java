package com.couserver.inventory.dto;

import java.util.List;

// 개별 합성 요청 (바디 자체가 없거나 materialInventoryIds가 null이면 서버가 재료를 자동 선택한다)
public record SynthesizeRequest(List<Long> materialInventoryIds) {
}