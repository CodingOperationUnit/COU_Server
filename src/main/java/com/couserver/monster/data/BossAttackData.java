package com.couserver.monster.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BossAttackData(
        int bossAttackId,
        int monsterId,
        BossAttackType bossAttackType,
        float bossAttackCooldown,
        float bossAttackRange,
        int bossAttackDamage
) {
}
