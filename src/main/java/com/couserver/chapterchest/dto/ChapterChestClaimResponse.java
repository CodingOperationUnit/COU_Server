package com.couserver.chapterchest.dto;

import com.couserver.inventory.dto.EquipmentResponse;

import java.util.List;

// 받은 스테이지와 지급한 장비. 클라이언트는 받은 기록에 stageId를 넣고 장비를 인벤토리에 더한다
public record ChapterChestClaimResponse(
        int stageId,
        List<EquipmentResponse> rewardedItems) {
}
