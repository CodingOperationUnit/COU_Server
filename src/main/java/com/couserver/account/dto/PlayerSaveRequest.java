package com.couserver.account.dto;

import com.couserver.player.dto.PlayerStatSaveRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class PlayerSaveRequest {
    @NotNull @Valid
    private ProfileSaveRequest profile;

    // 서버 값과 다르면 의심 활동으로 기록만 한다. 기존 클라이언트 호환을 위해 필드만 남긴다
    @Deprecated
    private CurrencySaveRequest currency;

    @NotNull @Valid
    private StageProgressSaveRequest stageProgress;

    // 진화 스탯은 진화 API로만 바뀐다
    @Deprecated
    private PlayerStatSaveRequest playerStat;

    // 장비(inventoryList)는 인벤토리 API에서 저장하므로 받지 않는다
}