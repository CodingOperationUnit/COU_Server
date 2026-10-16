package com.couserver.evolution;

import com.couserver.player.entity.PlayerStatType;
import com.couserver.staticdata.dto.EvolutionData;
import com.couserver.staticdata.service.StaticDataService;
import com.couserver.support.IntegrationTestSupport;
import com.couserver.support.TestAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EvolutionApiTest extends IntegrationTestSupport {
    private static final String UPGRADE_URL = "/api/players/me/evolution/upgrade";
    private static final int INITIAL_ACCOUNT_LEVEL = 1;

    @Autowired
    private StaticDataService staticDataService;

    @Test
    @DisplayName("골드가 비용만큼 있으면 진화하고 골드가 차감된다")
    void upgradeOnce() throws Exception {
        TestAccount account = createAccount();
        EvolutionData evolution = staticDataService.getEvolution();
        int cost = evolution.costAt(0);
        PlayerStatType firstStat = evolution.statAt(0);
        setGoldAndGem(account, cost, 0);

        postAs(account, UPGRADE_URL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upgradedStatType").value(firstStat.name()))
                .andExpect(jsonPath("$.evolutionStep").value(1))
                .andExpect(jsonPath("$.goldSpent").value(cost))
                .andExpect(jsonPath("$.currency.currencyGold").value(0))
                .andExpect(jsonPath("$.playerStat." + statField(firstStat)).value(1));

        getAs(account, SAVE_URL)
                .andExpect(jsonPath("$.currency.currencyGold").value(0))
                .andExpect(jsonPath("$.playerStat." + statField(firstStat)).value(1));
    }

    @Test
    @DisplayName("골드가 부족하면 400이고 스탯과 골드는 그대로다")
    void upgradeWithoutEnoughGoldFails() throws Exception {
        TestAccount account = createAccount();
        EvolutionData evolution = staticDataService.getEvolution();
        int cost = evolution.costAt(0);
        setGoldAndGem(account, cost - 1, 0);

        postAs(account, UPGRADE_URL)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_ENOUGH_GOLD"));

        getAs(account, SAVE_URL)
                .andExpect(jsonPath("$.currency.currencyGold").value(cost - 1))
                .andExpect(jsonPath("$.playerStat." + statField(evolution.statAt(0))).value(0));
    }

    @Test
    @DisplayName("계정 레벨이 허용하는 단계까지 진화하면 다음 진화는 레벨 제한으로 거부된다")
    void upgradeBeyondLevelLimitFails() throws Exception {
        TestAccount account = createAccount();
        EvolutionData evolution = staticDataService.getEvolution();
        int allowedSteps = evolution.levelStepLimit(INITIAL_ACCOUNT_LEVEL);

        int totalCost = 0;
        for (int step = 0; step < allowedSteps; step++) {
            totalCost += evolution.costAt(step);
        }
        setGoldAndGem(account, totalCost, 0);

        for (int step = 0; step < allowedSteps; step++) {
            postAs(account, UPGRADE_URL).andExpect(status().isOk());
        }

        postAs(account, UPGRADE_URL)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EVOLUTION_LEVEL_LOCKED"));

        getAs(account, SAVE_URL)
                .andExpect(jsonPath("$.currency.currencyGold").value(0))
                .andExpect(jsonPath("$.profile.accountLevel").value(INITIAL_ACCOUNT_LEVEL));
    }

    // 진화 스탯 종류에 해당하는 응답 필드 이름
    private String statField(PlayerStatType type) {
        return switch (type) {
            case ATTACK -> "playerStatAttackLevel";
            case HP -> "playerStatHpLevel";
            case DEFENSE -> "playerStatDefenseLevel";
            case POTION_RECOVERY -> "playerStatPotionRecoveryLevel";
        };
    }
}