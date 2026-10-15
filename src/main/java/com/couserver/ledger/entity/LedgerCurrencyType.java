package com.couserver.ledger.entity;

// 원장에 기록하는 재화 종류. 스태미나는 시간이 지나면 저절로 회복되는 값이라 기록하지 않는다
public enum LedgerCurrencyType {
    GOLD,
    GEM
}