package com.couserver.audit;

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

// 기록은 별도 스레드에서 저장되므로 Awaitility로 기다린다
class SuspiciousActivityRecordTest extends IntegrationTestSupport {
    private static final Duration WAIT = Duration.ofSeconds(5);

    @Autowired
    private StaticDataService staticDataService;

    @Test
    @DisplayName("save에 서버와 다른 재화를 보내면 불일치가 기록된다")
    void saveValueMismatchIsRecorded() throws Exception {
        TestAccount account = createAccount();
        setGoldAndGem(account, 100, 0);

        Map<String, Map<String, Object>> request = saveRequestFromCurrentSave(account);
        request.get("currency").put("currencyGold", 999_999_999);
        putAs(account, SAVE_URL, request).andExpect(status().isOk());

        await().atMost(WAIT).untilAsserted(() ->
                assertThat(details(account, "SAVE_VALUE_MISMATCH"))
                        .anySatisfy(detail -> assertThat(detail).contains("currencyGold")));
    }

    @Test
    @DisplayName("save로 미해금 스테이지를 고르면 거부되고 기록된다")
    void lockedStageSelectionIsRecorded() throws Exception {
        TestAccount account = createAccount();

        Map<String, Map<String, Object>> request = saveRequestFromCurrentSave(account);
        request.get("stageProgress").put("currentStageId", 5);
        request.get("stageProgress").put("maxClearedStageId", 4);
        putAs(account, SAVE_URL, request).andExpect(status().isBadRequest());

        await().atMost(WAIT).untilAsserted(() ->
                assertThat(details(account, "SAVE_LOCKED_STAGE")).hasSize(1));
    }

    @Test
    @DisplayName("전투 결과가 상한을 넘으면 상한까지만 지급되고 기록된다")
    void battleResultOverLimitIsRecorded() throws Exception {
        TestAccount account = createAccount();
        String enterBody = bodyOf(postAs(account, "/api/battles",
                Map.of("stageId", staticDataService.getFirstStageId())).andExpect(status().isOk()));
        Number battleId = JsonPath.read(enterBody, "$.battleId");

        Map<String, Object> result = new HashMap<>();
        result.put("victory", true);
        result.put("seconds", 3);
        result.put("kills", 0);
        result.put("gold", Integer.MAX_VALUE);
        result.put("rewardBoxes", 0);
        result.put("skillKills", Map.of());
        result.put("skillUpgrades", Map.of());
        result.put("pickups", Map.of());
        result.put("luckTrains", 0);

        String body = bodyOf(postAs(account, "/api/battles/" + battleId.longValue() + "/result", result)
                .andExpect(status().isOk()));
        Integer grantedGold = JsonPath.read(body, "$.grantedGold");
        assertThat(grantedGold).isLessThan(Integer.MAX_VALUE);

        await().atMost(WAIT).untilAsserted(() ->
                assertThat(details(account, "BATTLE_RESULT_OVER_LIMIT"))
                        .anySatisfy(detail -> assertThat(detail).contains("gold")));
    }

    @Test
    @DisplayName("서버 값 그대로 보낸 정상 save는 기록되지 않는다")
    void normalSaveIsNotRecorded() throws Exception {
        TestAccount account = createAccount();
        putAs(account, SAVE_URL, saveRequestFromCurrentSave(account)).andExpect(status().isOk());

        // 기록이 생기지 않는 것을 확인하려면 비동기 저장이 끝날 시간을 둔 뒤 확인한다
        await().during(Duration.ofSeconds(1)).atMost(WAIT).untilAsserted(() ->
                assertThat(details(account, null)).isEmpty());
    }

    // type이 null이면 해당 플레이어의 모든 기록
    private List<String> details(TestAccount account, String type) {
        if (type == null) {
            return jdbcTemplate.queryForList(
                    "SELECT detail FROM suspicious_activity WHERE player_id = ?",
                    String.class, account.playerId());
        }
        return jdbcTemplate.queryForList(
                "SELECT detail FROM suspicious_activity WHERE player_id = ? AND activity_type = ?",
                String.class, account.playerId(), type);
    }
}