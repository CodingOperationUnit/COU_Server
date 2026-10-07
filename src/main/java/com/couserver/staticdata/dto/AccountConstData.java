package com.couserver.staticdata.dto;

public record AccountConstData(
        int initialGold,
        int initialGem,
        int initialStamina,
        int maxStamina,
        int accountBaseRequiredExp,
        int accountRequiredExpIncrement,
        int maxAccountLevel,
        int battleStaminaCost) {
}
