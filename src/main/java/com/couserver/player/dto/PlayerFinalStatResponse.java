package com.couserver.player.dto;

public record PlayerFinalStatResponse(
        int finalAttack,
        int finalHp,
        int finalDefense,
        int finalPotionRecovery,
        // 아래는 기초 스탯 그대로 (장비·진화 반영 없음)
        int criticalDamage,
        int criticalChance,
        int skillDamage,
        float moveSpeed,
        float maxMoveSpeed,
        float lootRadius,
        Breakdown breakdown
) {
    // 출처별 내역 (계산 경로 확인용)
    public record Breakdown(
            StatSource base,
            StatSource equipment,
            StatSource evolution
    ){ }

    // 한 출처가 더해주는 값
    public record StatSource(
            int attack,
            int hp,
            int defense,
            int potionRecovery
    ) {
        public static final StatSource ZERO = new StatSource(0, 0, 0, 0);

        public StatSource plus (StatSource other){
            return new StatSource(
                    attack + other.attack,
                    hp + other.hp,
                    defense + other.defense,
                    potionRecovery + other.potionRecovery
            );
        }
    }


}
