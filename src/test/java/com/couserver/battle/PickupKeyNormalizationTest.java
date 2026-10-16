package com.couserver.battle;

import com.couserver.staticdata.service.StaticDataService;
import com.couserver.support.IntegrationTestSupport;
import com.couserver.support.TestAccount;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 3-a: 업적 pickup 키를 대소문자만 바꿔 나눠 보내도 상한을 넘지 못한다 (설계안 5.3)
class PickupKeyNormalizationTest extends IntegrationTestSupport {
    private static final int BOMB_ACHIEVEMENT_ID = 62001;         // 폭탄 획득
    private static final int LUCK_TRAIN_ACHIEVEMENT_ID = 67001;   // 행운열차 완주
    private static final int HUGE = 1000;                         // 어떤 상한보다도 큰 값

    @Autowired
    private StaticDataService staticDataService;

    @Test
    @DisplayName("대소문자만 다른 키로 나눠 보내도 키 하나로 보낸 것과 같은 진행도만 인정된다 (F10)")
    void splitPickupKeysDoNotMultiplyProgress() throws Exception {
        TestAccount single = createAccount();
        TestAccount split = createAccount();

        submitVictory(single, Map.of("Bomb", HUGE), 0);
        submitVictory(split, Map.of("Bomb", HUGE, "bomb", HUGE, "BOMB", HUGE), 0);

        int singleProgress = progress(single, BOMB_ACHIEVEMENT_ID);
        assertThat(singleProgress).as("상한으로 잘린 값").isPositive().isLessThan(HUGE);
        assertThat(progress(split, BOMB_ACHIEVEMENT_ID)).isEqualTo(singleProgress);
    }

    @Test
    @DisplayName("행운상자 키를 나눠 보내도 행운열차 횟수는 행운상자 상한을 넘지 않는다 (F10)")
    void splitLuckyBoxKeysDoNotMultiplyLuckTrains() throws Exception {
        TestAccount single = createAccount();
        TestAccount split = createAccount();

        submitVictory(single, Map.of("LuckyBox", HUGE), HUGE);
        submitVictory(split, Map.of("LuckyBox", HUGE, "luckybox", HUGE, "LUCKYBOX", HUGE), HUGE);

        int singleProgress = progress(single, LUCK_TRAIN_ACHIEVEMENT_ID);
        assertThat(singleProgress).as("행운상자 상한으로 잘린 값").isPositive().isLessThan(HUGE);
        assertThat(progress(split, LUCK_TRAIN_ACHIEVEMENT_ID)).isEqualTo(singleProgress);
    }

    @Test
    @DisplayName("상한 안의 정상 값은 그대로 인정된다")
    void normalPickupIsCounted() throws Exception {
        TestAccount account = createAccount();

        submitVictory(account, Map.of("Bomb", 2), 0);

        assertThat(progress(account, BOMB_ACHIEVEMENT_ID)).isEqualTo(2);
    }

    @Test
    @DisplayName("모르는 키는 무시한다")
    void unknownPickupKeyIsIgnored() throws Exception {
        TestAccount account = createAccount();

        submitVictory(account, Map.of("Unknown", 5), 0);

        assertThat(progress(account, BOMB_ACHIEVEMENT_ID)).isZero();
    }

    @Test
    @DisplayName("같은 종류의 키를 여러 개 보내면 의심 활동으로 기록된다")
    void duplicatePickupKeysAreRecorded() throws Exception {
        TestAccount account = createAccount();

        submitVictory(account, Map.of("Bomb", 1, "bomb", 1, "BOMB", 1), 0);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(jdbcTemplate.queryForList(
                        "SELECT detail FROM suspicious_activity WHERE player_id = ? AND activity_type = 'BATTLE_RESULT_OVER_LIMIT'",
                        String.class, account.playerId()))
                        .anySatisfy(detail -> assertThat(detail).contains("keys=3")));
    }

    private void submitVictory(TestAccount account, Map<String, Integer> pickups, int luckTrains) throws Exception {
        String enterBody = bodyOf(postAs(account, "/api/battles",
                Map.of("stageId", staticDataService.getFirstStageId())).andExpect(status().isOk()));
        Number battleId = JsonPath.read(enterBody, "$.battleId");

        Map<String, Object> result = new HashMap<>();
        result.put("victory", true);
        result.put("seconds", 3);
        result.put("kills", 0);
        result.put("gold", 0);
        result.put("rewardBoxes", 0);
        result.put("skillKills", Map.of());
        result.put("skillUpgrades", Map.of());
        result.put("pickups", pickups);
        result.put("luckTrains", luckTrains);

        postAs(account, "/api/battles/" + battleId.longValue() + "/result", result)
                .andExpect(status().isOk());
    }

    private int progress(TestAccount account, int achievementId) throws Exception {
        String body = bodyOf(getAs(account, "/api/players/me/achievements").andExpect(status().isOk()));
        List<Number> values = JsonPath.read(body, "$[?(@.achievementId == " + achievementId + ")].progress");
        return values.get(0).intValue();
    }
}