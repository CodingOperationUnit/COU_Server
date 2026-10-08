package com.couserver.staticdata.dto;

public record AccountConstData(
        int initialGold,
        int initialGem,
        int initialStamina,
        int maxStamina,
        int accountBaseRequiredExp,
        int accountRequiredExpIncrement,
        int maxAccountLevel,
        int battleStaminaCost,
        int staminaRecoverySeconds,
        int accountExpPerKill,
        int accountExpPerSecond,
        int luckTrainGoldMax) {

    // level에서 다음 레벨까지 필요한 계정 경험치. 클라이언트 AccountConstData.GetRequiredExp와 같은 공식
    public int requiredExp(int level) {
        return accountBaseRequiredExp + accountRequiredExpIncrement * (level - 1);
    }
}
