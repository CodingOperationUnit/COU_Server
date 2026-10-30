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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AchievementApiTest extends IntegrationTestSupport {
    private static final String ACHIEVEMENTS_URL = "/api/players/me/achievements";
    private static final String CLAIM_ALL_URL = ACHIEVEMENTS_URL + "/claim-all";
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

    @Test
    @DisplayName("모두 받기: 여러 업적의 대기 단계를 한 번에 받고, 합계가 보상표와 같다")
    void claimAllMultipleAchievements() throws Exception {
        TestAccount account = createAccount();
        List<AchievementData> picked = multiStepAchievements();
        AchievementData full = picked.get(0);      // 모든 단계 완료
        AchievementData partial = picked.get(1);   // 1단계만 완료
        setProgress(account, full, lastTarget(full), 0);
        setProgress(account, partial, partial.targetValue().get(0), 0);

        long gold = reward(full, 0, steps(full), AchievementRewardType.GOLD)
                + reward(partial, 0, 1, AchievementRewardType.GOLD);
        long gem = reward(full, 0, steps(full), AchievementRewardType.GEM)
                + reward(partial, 0, 1, AchievementRewardType.GEM);
        AccountConstData accountConst = staticDataService.getAccountConst();

        postAs(account, CLAIM_ALL_URL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGold").value(gold))
                .andExpect(jsonPath("$.totalGem").value(gem))
                .andExpect(jsonPath("$.claimedStepCount").value(steps(full) + 1))
                .andExpect(jsonPath("$.achievements[*].achievementId")
                        .value(contains(full.achievementId(), partial.achievementId())))   // 업적 ID 순서
                .andExpect(jsonPath("$.achievements[0].claimedStep").value(steps(full)))
                .andExpect(jsonPath("$.achievements[1].claimedStep").value(1))
                .andExpect(jsonPath("$.currency.currencyGold").value(accountConst.initialGold() + gold))
                .andExpect(jsonPath("$.currency.currencyGem").value(accountConst.initialGem() + gem));
    }

    @Test
    @DisplayName("모두 받기: 두 번째 호출은 받을 것이 없어 NOTHING_TO_CLAIM")
    void claimAllTwiceFails() throws Exception {
        TestAccount account = createAccount();
        AchievementData data = multiStepAchievements().get(0);
        setProgress(account, data, lastTarget(data), 0);

        postAs(account, CLAIM_ALL_URL).andExpect(status().isOk());
        postAs(account, CLAIM_ALL_URL)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTHING_TO_CLAIM"));
    }

    @Test
    @DisplayName("모두 받기: 진행도가 없는 계정은 NOTHING_TO_CLAIM")
    void claimAllWithoutProgressFails() throws Exception {
        TestAccount account = createAccount();

        postAs(account, CLAIM_ALL_URL)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTHING_TO_CLAIM"));
    }

    @Test
    @DisplayName("모두 받기: 단건으로 먼저 받은 단계는 다시 지급하지 않는다")
    void claimAllAfterSingleClaim() throws Exception {
        TestAccount account = createAccount();
        AchievementData data = multiStepAchievements().get(0);
        setProgress(account, data, lastTarget(data), 0);

        postAs(account, claimUrl(data.achievementId())).andExpect(status().isOk());   // 1단계 단건 수령

        long restGold = reward(data, 1, steps(data), AchievementRewardType.GOLD);
        long restGem = reward(data, 1, steps(data), AchievementRewardType.GEM);
        long allGold = reward(data, 0, steps(data), AchievementRewardType.GOLD);
        long allGem = reward(data, 0, steps(data), AchievementRewardType.GEM);
        AccountConstData accountConst = staticDataService.getAccountConst();

        postAs(account, CLAIM_ALL_URL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGold").value(restGold))
                .andExpect(jsonPath("$.totalGem").value(restGem))
                .andExpect(jsonPath("$.claimedStepCount").value(steps(data) - 1))
                .andExpect(jsonPath("$.currency.currencyGold").value(accountConst.initialGold() + allGold))
                .andExpect(jsonPath("$.currency.currencyGem").value(accountConst.initialGem() + allGem));
    }

    @Test
    @DisplayName("모두 받기: 동시에 두 번 호출해도 한 번만 지급된다")
    void claimAllConcurrently() throws Exception {
        TestAccount account = createAccount();
        AchievementData data = multiStepAchievements().get(0);
        setProgress(account, data, lastTarget(data), 0);

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            tasks.add(() -> postAs(account, CLAIM_ALL_URL).andReturn().getResponse().getStatus());
        }
        List<Integer> statuses = runConcurrently(tasks);

        assertThat(statuses).containsExactlyInAnyOrder(200, 400);
        AccountConstData accountConst = staticDataService.getAccountConst();
        getAs(account, "/api/players/me/save")
                .andExpect(jsonPath("$.currency.currencyGold").value(
                        accountConst.initialGold() + reward(data, 0, steps(data), AchievementRewardType.GOLD)))
                .andExpect(jsonPath("$.currency.currencyGem").value(
                        accountConst.initialGem() + reward(data, 0, steps(data), AchievementRewardType.GEM)));
    }

    @Test
    @DisplayName("모두 받기: 원장은 업적별·재화 종류별로 한 줄씩 남는다")
    void claimAllLedgerPerAchievement() throws Exception {
        TestAccount account = createAccount();
        List<AchievementData> picked = multiStepAchievements();   // 업적 ID 오름차순
        for (AchievementData data : picked) {
            setProgress(account, data, lastTarget(data), 0);
        }

        postAs(account, CLAIM_ALL_URL).andExpect(status().isOk());

        // 기대: 업적 ID 순서대로, 업적마다 골드(있으면) → 젬(있으면)
        List<String> expected = new ArrayList<>();
        for (AchievementData data : picked) {
            long gold = reward(data, 0, steps(data), AchievementRewardType.GOLD);
            long gem = reward(data, 0, steps(data), AchievementRewardType.GEM);
            if (gold > 0) expected.add(data.achievementId() + ":GOLD:" + gold);
            if (gem > 0) expected.add(data.achievementId() + ":GEM:" + gem);
        }

        List<String> actual = jdbcTemplate.query(
                "SELECT ref_id, currency_type, delta FROM currency_ledger "
                        + "WHERE player_id = ? AND reason = 'ACHIEVEMENT_REWARD' ORDER BY id",
                (rs, i) -> rs.getLong("ref_id") + ":" + rs.getString("currency_type") + ":" + rs.getLong("delta"),
                account.playerId());

        assertThat(actual).containsExactlyElementsOf(expected);
    }

    // 테스트 준비: 업적 진행값과 수령 단계를 직접 넣는다 (전투로는 처치 수 상한 때문에 여러 단계를 만들기 어렵다)
    private void setProgress(TestAccount account, AchievementData data, long progress, int claimedStep) {
        jdbcTemplate.update(
                "INSERT INTO player_achievement (player_id, achievement_id, player_achievement_progress, player_achievement_claimed_step) "
                        + "VALUES (?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE player_achievement_progress = VALUES(player_achievement_progress), "
                        + "player_achievement_claimed_step = VALUES(player_achievement_claimed_step)",
                account.playerId(), data.achievementId(), progress, claimedStep);
    }

    // 단계가 2개 이상인 업적 2개 (업적 ID 오름차순)
    private List<AchievementData> multiStepAchievements() {
        List<AchievementData> picked = staticDataService.getAchievements().values().stream()
                .filter(data -> data.targetValue().size() >= 2)
                .sorted(Comparator.comparingInt(AchievementData::achievementId))
                .limit(2)
                .toList();
        assertThat(picked).as("테스트에 쓸 다단계 업적이 2개 이상 있어야 한다").hasSize(2);
        return picked;
    }

    private static int steps(AchievementData data) {
        return data.targetValue().size();
    }

    private static long lastTarget(AchievementData data) {
        return data.targetValue().get(steps(data) - 1);
    }

    // fromStep 이상 toStep 미만 단계의 보상 중 지정한 종류의 합
    private static long reward(AchievementData data, int fromStep, int toStep, AchievementRewardType type) {
        long sum = 0;
        for (int step = fromStep; step < toStep; step++) {
            if (data.rewardItemType().get(step) == type) {
                sum += data.rewardAmount().get(step);
            }
        }
        return sum;
    }

    private String claimUrl(int achievementId) {
        return ACHIEVEMENTS_URL + "/" + achievementId + "/claim";
    }
}