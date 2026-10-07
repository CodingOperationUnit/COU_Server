package com.couserver.player.entity;

// 진화로 올릴 수 있는 능력치 enum. 선언 순서 = 진화 순서
// 공격력 -> 체력 -> 방어력 -> 포션 회복량 -> (다시 공격..)
public enum PlayerStatType {
    ATTACK,
    HP,
    DEFENSE,
    POTION_RECOVERY;

    public static PlayerStatType ofStep(int step){
        PlayerStatType[] order = values();
        return order[step % order.length];
    }
}
