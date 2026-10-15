package com.couserver.staticdata.dto;

public enum AchievementConditionType {
    KILLBYSKILL,
    PICKUPDROPITEM,
    UPGRADESKILL,
    GAINBATTLEEXP,
    GAINBATTLEGOLD,   // 전투에서 얻은 골드 누적
    CLEARSTAGE,       // 스테이지 클리어 횟수 누적
    PLAYLUCKTRAIN     // 행운열차 완주 횟수 누적
}