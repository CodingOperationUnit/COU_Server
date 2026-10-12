package com.couserver.player.entity;

// 진화로 올릴 수 있는 능력치 종류
// 진화 순서는 Evolution 시트의 evolutionStatOrder가 정한다 (이 선언 순서와 무관)
public enum PlayerStatType {
    ATTACK,
    HP,
    DEFENSE,
    POTION_RECOVERY;
}
