package com.couserver.account.dto;

import com.couserver.account.entity.PlayerProfile;
import com.couserver.inventory.entity.Equipment;
import java.util.List;
import lombok.Getter;

@Getter
public class PlayerProfileResponse {
    private final Long playerId;
    private final Integer accountId;
    private final String playerNickname;
    private final int accountLevel;
    private final int accountExp;
    private final Long equippedWeaponInventoryId;
    private final Long equippedArmorInventoryId;
    private final Long equippedBeltInventoryId;
    private final Long equippedGlovesInventoryId;
    private final Long equippedNecklaceInventoryId;
    private final Long equippedShoesInventoryId;

    public PlayerProfileResponse(PlayerProfile profile, List<Equipment> equipments) {
        this.playerId = profile.getPlayerId();
        this.accountId = profile.getAccount().getAccountId();
        this.playerNickname = profile.getPlayerNickname();
        this.accountLevel = profile.getAccountLevel();
        this.accountExp = profile.getAccountExp();

        Long weapon = null, armor = null, belt = null, gloves = null, necklace = null, shoes = null;

        for (Equipment equipment : equipments) {
            if (!equipment.isEquipped()) {
                continue;
            }
            Long id = equipment.getId();
            // 목록이 id 오름차순이라, 같은 슬롯에 2개가 있으면 먼저 나온(작은 id) 장비만 사용
            switch (equipment.getItem().getSlotType()) {
                case WEAPON -> { if (weapon == null) weapon = id; }
                case ARMOR -> { if (armor == null) armor = id; }
                case BELT -> { if (belt == null) belt = id; }
                case GLOVES -> { if (gloves == null) gloves = id; }
                case NECKLACE -> { if (necklace == null) necklace = id; }
                case SHOES -> { if (shoes == null) shoes = id; }
            }
        }

        this.equippedWeaponInventoryId = weapon;
        this.equippedArmorInventoryId = armor;
        this.equippedBeltInventoryId = belt;
        this.equippedGlovesInventoryId = gloves;
        this.equippedNecklaceInventoryId = necklace;
        this.equippedShoesInventoryId = shoes;
    }
}