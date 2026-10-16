package com.couserver.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;

@Slf4j
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

    // 회복을 계산한 스태미나와 회복 기준 시각
    public record EnergyState(int energy, Instant updatedAt) {
    }

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
        this.currencyGold = addCapped("gold", currencyGold, amount);
    }

    public void spendGem(int amount) {
        this.currencyGem -= amount;
    }

    public void addGem(int amount) {
        this.currencyGem = addCapped("gem", currencyGem, amount);
    }

    // 지금 시각까지 회복한 스태미나를 계산한다. 엔티티는 바꾸지 않는다 (조회 응답용)
    public EnergyState energyAt(Instant now, int maxStamina, int recoverySeconds) {
        if (currencyEnergyUpdatedAt == null || currencyEnergy >= maxStamina) {
            return new EnergyState(currencyEnergy, now);
        }
        long recovered = Duration.between(currencyEnergyUpdatedAt, now).getSeconds() / recoverySeconds;
        if (recovered <= 0) {
            return new EnergyState(currencyEnergy, currencyEnergyUpdatedAt);
        }
        if (currencyEnergy + recovered >= maxStamina) {
            return new EnergyState(maxStamina, now);
        }
        return new EnergyState(currencyEnergy + (int) recovered,
                currencyEnergyUpdatedAt.plusSeconds(recovered * recoverySeconds));
    }

    // 마지막 갱신 이후 지난 시간만큼 스태미나를 회복한다. 회복하고 남은 시간은 다음 회복에 이어서 쓴다
    public void recoverEnergy(Instant now, int maxStamina, int recoverySeconds) {
        EnergyState state = energyAt(now, maxStamina, recoverySeconds);
        this.currencyEnergy = state.energy();
        this.currencyEnergyUpdatedAt = state.updatedAt();
    }

    // 회복을 먼저 계산한 뒤 부른다. 최대치에서 차감하면 회복 시각이 이미 now라 그때부터 회복한다
    public void spendEnergy(int amount) {
        this.currencyEnergy -= amount;
    }

    // dev 전용: 스태미나를 최대치로 채운다
    public void refillEnergy(int maxStamina, Instant now) {
        this.currencyEnergy = maxStamina;
        this.currencyEnergyUpdatedAt = now;
    }

    // int 최대값을 넘으면 음수가 되므로 최대값에서 멈춘다. 정상 플레이로는 도달하기 어려워 로그를 남긴다
    private int addCapped(String name, int current, int amount) {
        long result = (long) current + amount;
        if (result > Integer.MAX_VALUE) {
            log.warn("재화 {} 보유량이 최대값을 넘어 최대값으로 맞춥니다. playerId={} current={} amount={}",
                    name, playerId, current, amount);
            return Integer.MAX_VALUE;
        }
        return (int) result;
    }
}