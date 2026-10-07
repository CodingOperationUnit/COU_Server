package com.couserver.account.dto;

import com.couserver.player.dto.PlayerStatSaveRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class PlayerSaveRequest {
    @NotNull @Valid
    private ProfileSaveRequest profile;

    @NotNull @Valid
    private CurrencySaveRequest currency;

    @NotNull @Valid
    private StageProgressSaveRequest stageProgress;

    @NotNull @Valid
    private PlayerStatSaveRequest playerStat;

    // inventoryList는 18단계에서 추가
}