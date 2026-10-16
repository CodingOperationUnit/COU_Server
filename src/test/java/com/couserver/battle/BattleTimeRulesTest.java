package com.couserver.battle;

import com.couserver.battle.service.BattleTimePolicy;
import com.couserver.staticdata.dto.MonsterType;
import com.couserver.staticdata.dto.StageData;
import com.couserver.staticdata.dto.StageRosterData;
import com.couserver.staticdata.dto.WaveEntryData;
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
import static org.hamcrest.Matchers.nullValue;
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
    @DisplayName("입장 직후 보고한 승리는 패배로 처리되어 클리어 EXP와 최고 클리어가 반영되지 않고 기록된다")
    void instantVictoryIsTreatedAsDefeat() throws Exception {
        setMultiplier(1.0);
        TestAccount account = createAccount();
        AccountConstData accountConst = staticDataService.getAccountConst();

        submit(account, true, 0, 0);

        getAs(account, SAVE_URL)
                .andExpect(jsonPath("$.stageProgress.maxClearedStageId").value(nullValue()))
                .andExpect(jsonPath("$.profile.accountExp").value(SECONDS * accountConst.accountExpPerSecond()));
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(count(account, "BATTLE_RESULT_TOO_FAST")).isEqualTo(1));
    }

    @Test
    @DisplayName("보스가 나올 만큼 시간이 흐를 수 있었다면 승리가 인정된다")
    void victoryIsAcceptedWhenBossCouldHaveAppeared() throws Exception {
        double bossAt = bossSpawnSeconds(staticDataService.getFirstStageId());
        setMultiplier(bossAt / 2.5);   // 입장 직후여도 계산상 보스 등장 시각의 2배 이상이 흐른 것으로 본다
        TestAccount account = createAccount();

        submit(account, true, 0, 0);

        getAs(account, SAVE_URL)
                .andExpect(jsonPath("$.stageProgress.maxClearedStageId").value(staticDataService.getFirstStageId()));
        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(count(account, "BATTLE_RESULT_TOO_FAST")).isZero());
    }

    @Test
    @DisplayName("짧은 시간에 큰 값을 보고하면 그 시각까지 나온 몬스터 기준으로 잘리고 기록된다")
    void valuesAreCappedByElapsedTime() throws Exception {
        setMultiplier(1.0);
        TestAccount account = createAccount();

        String body = submit(account, false, 500, 5000);

        Integer grantedGold = JsonPath.read(body, "$.grantedGold");
        assertThat(grantedGold).isLessThan(5000);
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(details(account, "BATTLE_RESULT_OVER_LIMIT"))
                        .anySatisfy(detail -> assertThat(detail).contains("kills")));
    }

    @Test
    @DisplayName("그 시각까지 나올 수 있는 범위의 정상적인 보고는 잘리지 않는다")
    void smallReportIsNotClamped() throws Exception {
        setMultiplier(1.0);
        TestAccount account = createAccount();
        AccountConstData accountConst = staticDataService.getAccountConst();
        int kills = 3;

        String body = submit(account, false, kills, 0);

        int expectedExp = kills * accountConst.accountExpPerKill() + SECONDS * accountConst.accountExpPerSecond();
        Integer grantedExp = JsonPath.read(body, "$.grantedExp");
        assertThat(grantedExp).isEqualTo(expectedExp);
        assertThat(count(account, "BATTLE_RESULT_OVER_LIMIT")).isZero();
    }

    private void setMultiplier(double value) {
        ReflectionTestUtils.setField(battleTimePolicy, "maxSpeedMultiplier", value);
    }

    // 스테이지의 보스가 처음 나오는 시각(초)
    private double bossSpawnSeconds(int stageId) {
        StageData stage = staticDataService.getStages().get(stageId);
        Map<String, StageRosterData> roster = staticDataService.getStageRosters().get(stageId);
        return staticDataService.getWaves().get(stage.waveId()).stream()
                .filter(entry -> staticDataService.getMonsters()
                        .get(roster.get(entry.monsterSlot()).monsterId()).monsterType() == MonsterType.BOSS)
                .mapToDouble(WaveEntryData::patternStartTime)
                .min()
                .orElseThrow();
    }

    // 첫 스테이지에 입장해 바로 결과를 제출하고 응답 본문을 돌려준다
    private String submit(TestAccount account, boolean victory, int kills, int gold) throws Exception {
        String enterBody = bodyOf(postAs(account, "/api/battles",
                Map.of("stageId", staticDataService.getFirstStageId())).andExpect(status().isOk()));
        Number battleId = JsonPath.read(enterBody, "$.battleId");

        Map<String, Object> result = new HashMap<>();
        result.put("victory", victory);
        result.put("seconds", SECONDS);
        result.put("kills", kills);
        result.put("gold", gold);
        result.put("rewardBoxes", 0);
        result.put("skillKills", Map.of());
        result.put("skillUpgrades", Map.of());
        result.put("pickups", Map.of());
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