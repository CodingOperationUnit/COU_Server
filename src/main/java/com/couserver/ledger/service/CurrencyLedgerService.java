package com.couserver.ledger.service;

import com.couserver.account.entity.Currency;
import com.couserver.ledger.entity.CurrencyLedger;
import com.couserver.ledger.entity.LedgerCurrencyType;
import com.couserver.ledger.entity.LedgerReason;
import com.couserver.ledger.repository.CurrencyLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

// gold·gem을 바꾸면서 원장에 기록하는 유일한 통로.
// - 호출하는 쪽의 트랜잭션 안에서만 동작한다 (MANDATORY). 재화 행을 잠그지 않고 부르는 코드가 생기는 것을 막고,
//   원장 기록이 실패하면 재화 변경도 함께 롤백되어 원장과 잔액이 어긋나지 않는다
// - 기록하는 증감은 요청 금액이 아니라 실제로 바뀐 양이다 (오버플로 가드로 최대값에서 멈춘 경우도 맞다)
// - 증감이 0이면 기록하지 않는다
// 사용하는 쪽은 Currency 행을 잠근(findForUpdate) 뒤에 부른다
@Service
@RequiredArgsConstructor
public class CurrencyLedgerService {
    private final CurrencyLedgerRepository repository;
    private final Clock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public void addGold(Currency currency, int amount, LedgerReason reason, Long refId) {
        int before = currency.getCurrencyGold();
        currency.addGold(amount);
        record(currency, LedgerCurrencyType.GOLD, before, currency.getCurrencyGold(), reason, refId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void spendGold(Currency currency, int amount, LedgerReason reason, Long refId) {
        int before = currency.getCurrencyGold();
        currency.spendGold(amount);
        record(currency, LedgerCurrencyType.GOLD, before, currency.getCurrencyGold(), reason, refId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void addGem(Currency currency, int amount, LedgerReason reason, Long refId) {
        int before = currency.getCurrencyGem();
        currency.addGem(amount);
        record(currency, LedgerCurrencyType.GEM, before, currency.getCurrencyGem(), reason, refId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void spendGem(Currency currency, int amount, LedgerReason reason, Long refId) {
        int before = currency.getCurrencyGem();
        currency.spendGem(amount);
        record(currency, LedgerCurrencyType.GEM, before, currency.getCurrencyGem(), reason, refId);
    }

    private void record(Currency currency, LedgerCurrencyType type, int before, int after,
                        LedgerReason reason, Long refId) {
        long delta = (long) after - before;
        if (delta == 0) {
            return;
        }
        repository.save(new CurrencyLedger(
                currency.getPlayerId(), type, delta, after, reason, refId, Instant.now(clock)));
    }
}