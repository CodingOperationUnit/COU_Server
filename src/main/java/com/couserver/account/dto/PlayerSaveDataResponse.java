package com.couserver.account.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class PlayerSaveDataResponse {
    private final PlayerProfileResponse profile;
    private final CurrencyResponse currency;
    private final List<Object> inventoryList;   // TODO(15단계): InventoryResponse 목록으로 교체
    private final StageProgressResponse stageProgress;
    private final PlayerStatResponse playerStat;
}