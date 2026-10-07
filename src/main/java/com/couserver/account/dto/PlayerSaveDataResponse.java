package com.couserver.account.dto;

import com.couserver.inventory.dto.InventoryResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class PlayerSaveDataResponse {
    private final PlayerProfileResponse profile;
    private final CurrencyResponse currency;
    private final List<InventoryItemResponse> inventoryList;
    private final StageProgressResponse stageProgress;
    private final PlayerStatResponse playerStat;
}