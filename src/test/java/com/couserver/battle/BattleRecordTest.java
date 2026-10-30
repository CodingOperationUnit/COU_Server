package com.couserver.battle;

import com.couserver.player.service.PlayerFinalStatService;
import com.couserver.staticdata.service.StaticDataService;
import com.couserver.support.IntegrationTestSupport;
import com.couserver.support.TestAccount;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 4-b-2: 전투 결과가 처리되면 통계 한 줄이 남는다
class BattleRecordTest extends IntegrationTestSupport {
    private static final String ENTER_URL = "/api/battles";
    private static final int SECONDS = 3;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private PlayerFinalStatService playerFinalStatService;

    @Test
    @DisplayName("전투 결과가 처리되면 인정한 값, 상한, 전투력이 기록된다")
    void recordIsSavedWithAcceptedValues() throws Exception {
        TestAccount account = createAccount();

        long battleId = submit(account, true, 10, 100, 1);

        Map<String, Object> row = recordOf(battleId);
        assertThat(((Number) row.get("player_id")).longValue()).isEqualTo(account.playerId());
        assertThat(((Number) row.get("stage_id")).intValue()).isEqualTo(staticDataService.getFirstStageId());
        assertThat(flag(row, "victory_reported")).isTrue();
        assertThat(flag(row, "victory_accepted")).isTrue();
        assertThat(((Number) row.get("seconds")).intValue()).isEqualTo(SECONDS);
        assertThat(((Number) row.get("kills")).intValue()).isEqualTo(10);
        assertThat(((Number) row.get("gold")).intValue()).isEqualTo(100);
        assertThat(((Number) row.get("reward_boxes")).intValue()).isEqualTo(1);
        assertThat(((Number) row.get("kills_limit")).intValue()).isGreaterThanOrEqualTo(10);
        assertThat(((Number) row.get("gold_limit")).intValue()).isGreaterThanOrEqualTo(100);
        assertThat(((Number) row.get("reward_boxes_limit")).intValue()).isGreaterThanOrEqualTo(1);
        assertThat(((Number) row.get("elapsed_seconds")).longValue()).isGreaterThanOrEqualTo(0);
        assertThat(((Number) row.get("combat_power")).intValue())
                .isEqualTo(playerFinalStatService.getFinalStat(account.playerId()).combatPower());
    }

    @Test
    @DisplayName("거부된 요청은 기록을 남기지 않고, 같은 전투는 한 줄만 남는다")
    void rejectedRequestsLeaveNoRecord() throws Exception {
        TestAccount owner = createAccount();
        TestAccount other = createAccount();
        String enterBody = bodyOf(postAs(owner, ENTER_URL,
                Map.of("stageId", staticDataService.getFirstStageId())).andExpect(status().isOk()));
        Number battleId = JsonPath.read(enterBody, "$.battleId");
        String resultUrl = ENTER_URL + "/" + battleId.longValue() + "/result";

        postAs(other, resultUrl, result(true, 0, 0, 0)).andExpect(status().isForbidden());
        postAs(owner, resultUrl, result(true, 0, 0, 0)).andExpect(status().isOk());
        postAs(owner, resultUrl, result(true, 0, 0, 0)).andExpect(status().isConflict());

        assertThat(recordCount(other)).isZero();
        assertThat(recordCount(owner)).isEqualTo(1);
    }

    @Test
    @DisplayName("최소 피해는 계산하지 않아 -1로 기록된다")
    void minDamageIsUnknown() throws Exception {
        TestAccount account = createAccount();

        long battleId = submit(account, false, 3, 0, 0);

        assertThat(((Number) recordOf(battleId).get("min_damage")).longValue()).isEqualTo(-1);
    }

    // 첫 스테이지에 입장해 바로 결과를 제출하고 전투 ID를 돌려준다
    private long submit(TestAccount account, boolean victory, int kills, int gold, int rewardBoxes) throws Exception {
        String enterBody = bodyOf(postAs(account, ENTER_URL,
                Map.of("stageId", staticDataService.getFirstStageId())).andExpect(status().isOk()));
        Number battleId = JsonPath.read(enterBody, "$.battleId");

        postAs(account, ENTER_URL + "/" + battleId.longValue() + "/result", result(victory, kills, gold, rewardBoxes))
                .andExpect(status().isOk());
        return battleId.longValue();
    }

    private Map<String, Object> result(boolean victory, int kills, int gold, int rewardBoxes) {
        Map<String, Object> request = new HashMap<>();
        request.put("victory", victory);
        request.put("seconds", SECONDS);
        request.put("kills", kills);
        request.put("gold", gold);
        request.put("rewardBoxes", rewardBoxes);
        request.put("quality", 0);
        request.put("skillKills", Map.of());
        request.put("skillUpgrades", Map.of());
        request.put("pickups", Map.of());
        request.put("luckTrains", 0);
        return request;
    }

    private Map<String, Object> recordOf(long battleId) {
        return jdbcTemplate.queryForMap("SELECT * FROM battle_record WHERE battle_id = ?", battleId);
    }

    private int recordCount(TestAccount account) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM battle_record WHERE player_id = ?", Integer.class, account.playerId());
    }

    // MySQL의 boolean은 드라이버에 따라 Boolean이나 숫자로 읽힌다
    private static boolean flag(Map<String, Object> row, String column) {
        Object value = row.get(column);
        return value instanceof Boolean b ? b : ((Number) value).intValue() != 0;
    }
}