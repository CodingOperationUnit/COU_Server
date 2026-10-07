package com.couserver.account.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "player_profile")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long playerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(nullable = false, unique = true, length = 20)
    private String playerNickname;

    @Column(nullable = false)
    private int accountLevel = 1;

    @Column(nullable = false)
    private int accountExp = 0;

    // 장착 장비 (FK → Inventory). 비어 있으면 Null
    private Integer equippedWeaponInventoryId;
    private Integer equippedArmorInventoryId;
    private Integer equippedBeltInventoryId;
    private Integer equippedGlovesInventoryId;
    private Integer equippedNecklaceInventoryId;
    private Integer equippedShoesInventoryId;

    public PlayerProfile(Account account, String playerNickname) {
        this.account = account;
        this.playerNickname = playerNickname;
    }
}
