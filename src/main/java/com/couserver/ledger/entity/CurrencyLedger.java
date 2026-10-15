package com.couserver.ledger.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

// 재화(gold, gem)가 바뀔 때마다 한 줄씩 남기는 기록. 수정하거나 지우지 않고 쌓기만 한다
@Getter
@Entity
@Table(name = "currency_ledger",
        indexes = @Index(name = "idx_currency_ledger_player", columnList = "playerId, createdAt"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CurrencyLedger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LedgerCurrencyType currencyType;

    // 지급은 양수, 사용은 음수. 요청 금액이 아니라 실제로 바뀐 양이다
    @Column(nullable = false)
    private long delta;

    @Column(nullable = false)
    private int balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LedgerReason reason;

    // 전투 ID, 상품 ID 같은 관련 ID. 없으면 null
    private Long refId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public CurrencyLedger(Long playerId, LedgerCurrencyType currencyType, long delta, int balanceAfter,
                          LedgerReason reason, Long refId, Instant createdAt) {
        this.playerId = playerId;
        this.currencyType = currencyType;
        this.delta = delta;
        this.balanceAfter = balanceAfter;
        this.reason = reason;
        this.refId = refId;
        this.createdAt = createdAt;
    }
}