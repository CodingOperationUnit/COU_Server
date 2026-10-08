package com.couserver.battle.dto;

import com.couserver.account.dto.CurrencyResponse;
import com.couserver.account.dto.PlayerProfileResponse;
import com.couserver.account.dto.StageProgressResponse;
import com.couserver.inventory.dto.EquipmentResponse;

import java.util.List;

// 지급 후 상태. 클라이언트는 이 값으로 결과창을 채우고 PlayerSaveData를 덮어쓴다
public record BattleResultResponse(
        int grantedGold,
        int grantedExp,
        PlayerProfileResponse profile,
        CurrencyResponse currency,
        StageProgressResponse stageProgress,
        StageRecordResponse stageRecord,
        List<EquipmentResponse> rewards) {
}
