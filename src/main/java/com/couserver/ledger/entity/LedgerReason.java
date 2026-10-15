package com.couserver.ledger.entity;

// 재화가 바뀐 사유. 재화를 바꾸는 기능이 생기면 여기에 추가한다
public enum LedgerReason {
    BATTLE_RESULT,        // 전투 결과 골드 지급 (refId = 전투 ID)
    SHOP_PURCHASE,        // 상점 구매 (refId = 상품 ID)
    EVOLUTION,            // 진화 골드 차감 (refId = 진화 후 단계)
    INVENTORY_LEVEL_UP,   // 장비 레벨업 골드 차감 (refId = 장비 ID)
    ACHIEVEMENT_REWARD    // 업적 보상 지급 (refId = 업적 ID)
}