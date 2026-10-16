package com.couserver.ledger.repository;

import com.couserver.ledger.entity.CurrencyLedger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyLedgerRepository extends JpaRepository<CurrencyLedger, Long> {
}