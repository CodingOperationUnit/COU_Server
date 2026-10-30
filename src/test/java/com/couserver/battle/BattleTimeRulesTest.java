package com.couserver.battle;

import com.couserver.battle.service.BattleTimePolicy;
import com.couserver.staticdata.dto.AccountConstData;
import com.couserver.staticdata.service.StaticDataService;
import com.couserver.support.IntegrationTestSupport;
import com.couserver.support.TestAccount;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 4-b-1: 전투 시간 검증. 테스트 프로파일은 시간 검증을 사실상 꺼 두므로(배속 한도 1000),
// 이 클래스가 한도를 실제 값으로 바꿔서 확인하고 끝나면 되돌린다
class BattleTimeRulesTest extends IntegrationTestSupport {
    private static final int SECONDS = 3;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private BattleTimePolicy battleTimePolicy;

    private double originalMultiplier;

    @BeforeEach
    void remember() {
        originalMultiplier = (Double) ReflectionTestUtils.getField(battleTimePolicy, "maxSpeedMultiplier");
    }

    @AfterEach
    void restore() {
        setMultiplier(originalMultiplier);
    }

    @Test
    @DisplayName("입장 직후 보고한 승리도 승리로 인정된다 (너무 빠른 승리 검사 없음)")
    void instantVictoryIsAccepted() throws Exception {
        setMultiplier(1.0);
        TestAccount account = createAccount();

        String body = submit(account, true, SECONDS, 0, Map.of());

        assertThat((Integer) JsonPath.read(body, "$.stageRecord.bestClearSeconds")).isEqualTo(SECONDS);
        getAs(account, SAVE_URL)
                .andExpect(jsonPath("$.stageProgress.maxClearedStageId").value(staticDataService.getFirstStageId()));
    }

    @Test
    @DisplayName("경과 시간보다 긴 전투 시간을 보고하면 잘리고, 상자 상한도 잘린 시간으로 계산되어 기록된다")
    void boxLimitFollowsElapsedTime() throws Exception {
        setMultiplier(1.0);
        TestAccount account = createAccount();
        // 상자 몇 개가 나올 만큼의 시간. 경과 시간(0초 + 허용 5초)으로 잘리면 상자가 0개라 폭탄 1개도 상한을 넘는다
        int seconds = (int) Math.ceil(staticDataService.getBattleConst().boxSpawnInterval() * 3);

        submit(account, false, seconds, 0, Map.of("Bomb", 1));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(details(account, "BATTLE_RESULT_OVER_LIMIT")).singleElement()
                        .satisfies(detail -> assertThat(detail).contains("seconds", "pickups.BOMB client=1 max=0")));
    }

    @Test
    @DisplayName("그 시각까지 나올 수 있는 범위의 정상적인 보고는 잘리지 않는다")
    void smallReportIsNotClamped() throws Exception {
        setMultiplier(1.0);
        TestAccount account = createAccount();
        AccountConstData accountConst = staticDataService.getAccountConst();
        int kills = 3;

        String body = submit(account, false, SECONDS, kills, Map.of());

        int expectedExp = kills * accountConst.accountExpPerKill() + SECONDS * accountConst.accountExpPerSecond();
        Integer grantedExp = JsonPath.read(body, "$.grantedExp");
        assertThat(grantedExp).isEqualTo(expectedExp);
        assertThat(count(account, "BATTLE_RESULT_OVER_LIMIT")).isZero();
    }

    private void setMultiplier(double value) {
        ReflectionTestUtils.setField(battleTimePolicy, "maxSpeedMultiplier", value);
    }

    // 첫 스테이지에 입장해 바로 결과를 제출하고 응답 본문을 돌려준다
    private String submit(TestAccount account, boolean victory, int seconds, int kills,
                          Map<String, Integer> pickups) throws Exception {
        String enterBody = bodyOf(postAs(account, "/api/battles",
                Map.of("stageId", staticDataService.getFirstStageId())).andExpect(status().isOk()));
        Number battleId = JsonPath.read(enterBody, "$.battleId");

        Map<String, Object> result = new HashMap<>();
        result.put("victory", victory);
        result.put("seconds", seconds);
        result.put("kills", kills);
        result.put("gold", 0);
        result.put("rewardBoxes", 0);
        result.put("quality", 0);
        result.put("skillKills", Map.of());
        result.put("skillUpgrades", Map.of());
        result.put("pickups", pickups);
        result.put("luckTrains", 0);

        return bodyOf(postAs(account, "/api/battles/" + battleId.longValue() + "/result", result)
                .andExpect(status().isOk()));
    }

    private int count(TestAccount account, String type) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM suspicious_activity WHERE player_id = ? AND activity_type = ?",
                Integer.class, account.playerId(), type);
    }

    private List<String> details(TestAccount account, String type) {
        return jdbcTemplate.queryForList(
                "SELECT detail FROM suspicious_activity WHERE player_id = ? AND activity_type = ?",
                String.class, account.playerId(), type);
    }
}