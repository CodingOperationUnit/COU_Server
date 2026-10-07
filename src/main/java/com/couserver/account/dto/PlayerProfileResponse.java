package com.couserver.account.dto;

import com.couserver.account.entity.PlayerProfile;
import lombok.Getter;

@Getter
public class PlayerProfileResponse {
    private final Long playerId;
    private final Integer accountId;
    private final String playerNickname;
    private final int accountLevel;
    private final int accountExp;
    private final Integer equippedWeaponInventoryId;
    private final Integer equippedArmorInventoryId;
    private final Integer equippedBeltInventoryId;
    private final Integer equippedGlovesInventoryId;
    private final Integer equippedNecklaceInventoryId;
    private final Integer equippedShoesInventoryId;

    public PlayerProfileResponse(PlayerProfile profile) {
        this.playerId = profile.getPlayerId();
        this.accountId = profile.getAccount().getAccountId();
        this.playerNickname = profile.getPlayerNickname();
        this.accountLevel = profile.getAccountLevel();
        this.accountExp = profile.getAccountExp();
        this.equippedWeaponInventoryId = profile.getEquippedWeaponInventoryId();
        this.equippedArmorInventoryId = profile.getEquippedArmorInventoryId();
        this.equippedBeltInventoryId = profile.getEquippedBeltInventoryId();
        this.equippedGlovesInventoryId = profile.getEquippedGlovesInventoryId();
        this.equippedNecklaceInventoryId = profile.getEquippedNecklaceInventoryId();
        this.equippedShoesInventoryId = profile.getEquippedShoesInventoryId();
    }
}