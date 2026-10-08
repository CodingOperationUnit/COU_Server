package com.couserver.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

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

    private Instant currencyEnergyUpdatedAt;

    public Currency(Long playerId, int currencyGold, int currencyGem, int currencyEnergy, Instant now) {
        this.playerId = playerId;
        this.currencyGold = currencyGold;
        this.currencyGem = currencyGem;
        this.currencyEnergy = currencyEnergy;
        this.currencyEnergyUpdatedAt = now;
    }

    public void spendGold(int amount) {
        this.currencyGold -= amount;
    }

    public void addGold(int amount) {
        this.currencyGold += amount;
    }

    public void spendGem(int amount) {
        this.currencyGem -= amount;
    }

    public void addGem(int amount) {
        this.currencyGem += amount;
    }

    // 마지막 갱신 이후 지난 시간만큼 스태미나를 회복한다. 회복하고 남은 시간은 다음 회복에 이어서 쓴다
    public void recoverEnergy(Instant now, int maxStamina, int recoverySeconds) {
        if (currencyEnergyUpdatedAt == null || currencyEnergy >= maxStamina) {
            currencyEnergyUpdatedAt = now;
            return;
        }

        long recovered = Duration.between(currencyEnergyUpdatedAt, now).getSeconds() / recoverySeconds;
        if (recovered <= 0) {
            return;
        }

        if (currencyEnergy + recovered >= maxStamina) {
            currencyEnergy = maxStamina;
            currencyEnergyUpdatedAt = now;
        } else {
            currencyEnergy += (int) recovered;
            currencyEnergyUpdatedAt = currencyEnergyUpdatedAt.plusSeconds(recovered * recoverySeconds);
        }
    }

    // 회복을 먼저 계산한 뒤 부른다. 최대치에서 차감하면 회복 시각이 이미 now라 그때부터 회복한다
    public void spendEnergy(int amount) {
        this.currencyEnergy -= amount;
    }

    public void update(int gold, int gem, int energy, Instant energyUpdatedAt) {
        this.currencyGold = gold;
        this.currencyGem = gem;
        this.currencyEnergy = energy;
        this.currencyEnergyUpdatedAt = energyUpdatedAt;
    }
}
