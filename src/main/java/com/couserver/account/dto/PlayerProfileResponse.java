package com.couserver.account.dto;

import com.couserver.account.entity.PlayerProfile;
import lombok.Getter;

// 장착 정보는 인벤토리 API(Equipment.equipped)에서 다룬다
@Getter
public class PlayerProfileResponse {
    private final Long playerId;
    private final Integer accountId;
    private final String playerNickname;
    private final int accountLevel;
    private final int accountExp;

    public PlayerProfileResponse(PlayerProfile profile) {
        this.playerId = profile.getPlayerId();
        this.accountId = profile.getAccount().getAccountId();
        this.playerNickname = profile.getPlayerNickname();
        this.accountLevel = profile.getAccountLevel();
        this.accountExp = profile.getAccountExp();
    }
}