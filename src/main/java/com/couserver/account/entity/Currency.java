package com.couserver.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "currency")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Currency {
    @Id
    private Long playerId; // PK = PlayerProfile의 PlayerId

    @Column(nullable = false)
    private int currencyGold;

    @Column(nullable = false)
    private int currencyGem;

    @Column(nullable = false)
    private int currencyEnergy;

    private LocalDateTime currencyEnergyUpdatedAt;

    public Currency(Long playerId, int currencyGold, int currencyGem, int currencyEnergy) {
        this.playerId = playerId;
        this.currencyGold = currencyGold;
        this.currencyGem = currencyGem;
        this.currencyEnergy = currencyEnergy;
        this.currencyEnergyUpdatedAt = LocalDateTime.now();
    }
}
