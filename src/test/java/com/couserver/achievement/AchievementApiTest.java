package com.couserver.achievement;

import com.couserver.staticdata.dto.AccountConstData;
import com.couserver.staticdata.dto.AchievementData;
import com.couserver.staticdata.dto.AchievementRewardType;
import com.couserver.staticdata.service.StaticDataService;
import com.couserver.support.IntegrationTestSupport;
import com.couserver.support.TestAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AchievementApiTest extends IntegrationTestSupport {
    private static final String ACHIEVEMENTS_URL = "/api/players/me/achievements";
    private static final int SKILL_KILL_ACHIEVEMENT_ID = 61001;   // 텀블러 처치 업적 (1단계 50킬)
    private static final int BATTLE_SECONDS = 3;

    @Autowired
    private StaticDataService staticDataService;

    @Test
    @DisplayName("신규 계정의 업적 목록은 전부 진행도 0이다")
    void listAchievements() throws Exception {
        TestAccount account = createAccount();

        getAs(account, ACHIEVEMENTS_URL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(staticDataService.getAchievements().size()))
                .andExpect(jsonPath("$[*].progress", everyItem(is(0))))
                .andExpect(jsonPath("$[*].claimedStep", everyItem(is(0))));
    }

    @Test
    @DisplayName("전투로 1단계를 달성하면 보상을 한 번 받을 수 있다")
    void claimAfterBattle() throws Exception {
        TestAccount account = createAccount();
        AchievementData achievement = staticDataService.getAchievements().get(SKILL_KILL_ACHIEVEMENT_ID);
        int firstTarget = achievement.targetValue().get(0);

        clearStage(account, staticDataService.getFirstStageId(), firstTarget, BATTLE_SECONDS,
                Map.of(achievement.targetSkillId(), firstTarget));

        getAs(account, ACHIEVEMENTS_URL)
                .andExpect(jsonPath("$[?(@.achievementId == %d)].progress", SKILL_KILL_ACHIEVEMENT_ID)
                        .value(contains(firstTarget)))
                .andExpect(jsonPath("$[?(@.achievementId == %d)].completedStep", SKILL_KILL_ACHIEVEMENT_ID)
                        .value(contains(1)));

        AccountConstData accountConst = staticDataService.getAccountConst();
        boolean goldReward = achievement.rewardItemType().get(0) == AchievementRewardType.GOLD;
        String rewardPath = goldReward ? "$.currency.currencyGold" : "$.currency.currencyGem";
        int before = goldReward ? accountConst.initialGold() : accountConst.initialGem();   // 전투 골드는 0으로 보냈다

        postAs(account, claimUrl(SKILL_KILL_ACHIEVEMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath(rewardPath).value(before + achievement.rewardAmount().get(0)))
                .andExpect(jsonPath("$.achievement.claimedStep").value(1));

        postAs(account, claimUrl(SKILL_KILL_ACHIEVEMENT_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTHING_TO_CLAIM"));
    }

    @Test
    @DisplayName("진행도가 없으면 보상을 받을 수 없다")
    void claimWithoutProgressFails() throws Exception {
        TestAccount account = createAccount();

        postAs(account, claimUrl(SKILL_KILL_ACHIEVEMENT_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTHING_TO_CLAIM"));
    }

    @Test
    @DisplayName("없는 업적의 보상을 받으면 404")
    void claimUnknownAchievementFails() throws Exception {
        TestAccount account = createAccount();
        int unknownId = Collections.max(staticDataService.getAchievements().keySet()) + 1;

        postAs(account, claimUrl(unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACHIEVEMENT_NOT_FOUND"));
    }

    private String claimUrl(int achievementId) {
        return ACHIEVEMENTS_URL + "/" + achievementId + "/claim";
    }
}