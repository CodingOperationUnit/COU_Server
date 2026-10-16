package com.couserver.battle.dto;

import com.couserver.account.dto.CurrencyResponse;
import com.couserver.account.dto.PlayerProfileResponse;
import com.couserver.inventory.dto.EquipmentResponse;

import java.util.List;

// 순찰 지급 후 상태. 최고 클리어 스테이지를 한 번 클리어한 평균 보상
public record PatrolResponse(
        int stageId,
        int grantedGold,
        int grantedExp,
        PlayerProfileResponse profile,
        CurrencyResponse currency,
        List<EquipmentResponse> rewards) {
}
