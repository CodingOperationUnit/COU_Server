package com.couserver.account.dto;

import com.couserver.player.dto.PlayerStatResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 장비 목록은 인벤토리 API(GET /api/inventory)에서 조회한다
@Getter
@RequiredArgsConstructor
public class PlayerSaveDataResponse {
    private final PlayerProfileResponse profile;
    private final CurrencyResponse currency;
    private final StageProgressResponse stageProgress;
    private final PlayerStatResponse playerStat;
}