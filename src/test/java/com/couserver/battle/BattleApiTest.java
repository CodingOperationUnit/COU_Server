package com.couserver.battle;

import com.couserver.staticdata.dto.AccountConstData;
import com.couserver.staticdata.dto.StageData;
import com.couserver.staticdata.service.StaticDataService;
import com.couserver.support.IntegrationTestSupport;
import com.couserver.support.TestAccount;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BattleApiTest extends IntegrationTestSupport {
    private static final String ENTER_URL = "/api/battles";

    // 1스테이지 상한(킬 약 895, 골드 약 11700, 상자 12)보다 충분히 작은 값. seconds는 "경과 시간 + 5초"까지 인정된다
    private static final int KILLS = 10;
    private static final int GOLD = 100;
    private static final int SECONDS = 3;
    private static final int REWARD_BOXES = 1;

    @Autowired
    private StaticDataService staticDataService;

    @Test
    @DisplayName("첫 스테이지에 입장하면 전투 ID를 받고 스태미나가 차감된다")
    void enterFirstStage() throws Exception {
        TestAccount account = createAccount();
        AccountConstData accountConst = staticDataService.getAccountConst();

        postAs(account, ENTER_URL, Map.of("stageId", staticDataService.getFirstStageId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.battleId").isNumber())
                .andExpect(jsonPath("$.currency.currencyEnergy")
                        .value(accountConst.initialStamina() - accountConst.battleStaminaCost()));
    }

    @Test
    @DisplayName("승리 결과를 제출하면 골드·경험치·진행도·기록·보상 장비가 반영된다")
    void submitVictoryResult() throws Exception {
        TestAccount account = createAccount();
        int firstStageId = staticDataService.getFirstStageId();
        AccountConstData accountConst = staticDataService.getAccountConst();
        StageData stage = staticDataService.getStages().get(firstStageId);
        int expectedExp = KILLS * accountConst.accountExpPerKill()
                + SECONDS * accountConst.accountExpPerSecond()
                + stage.clearAccountExp();

        long battleId = enter(account, firstStageId);

        postAs(account, resultUrl(battleId), victoryResult())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grantedGold").value(GOLD))
                .andExpect(jsonPath("$.grantedExp").value(expectedExp))
                .andExpect(jsonPath("$.currency.currencyGold").value(accountConst.initialGold() + GOLD))
                .andExpect(jsonPath("$.stageProgress.maxClearedStageId").value(firstStageId))
                .andExpect(jsonPath("$.stageRecord.stageId").value(firstStageId))
                .andExpect(jsonPath("$.stageRecord.bestSurvivalSeconds").value(SECONDS))
                .andExpect(jsonPath("$.rewards.length()").value(REWARD_BOXES));

        // 세이브와 인벤토리에도 반영되어 있어야 한다
        getAs(account, SAVE_URL)
                .andExpect(jsonPath("$.currency.currencyGold").value(accountConst.initialGold() + GOLD))
                .andExpect(jsonPath("$.stageProgress.maxClearedStageId").value(firstStageId))
                .andExpect(jsonPath("$.stageRecords[0].bestSurvivalSeconds").value(SECONDS));
        getAs(account, "/api/inventory")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(REWARD_BOXES));
    }

    @Test
    @DisplayName("같은 전투로 결과를 다시 제출하면 409이고 골드는 한 번만 지급된다")
    void submitResultTwiceFails() throws Exception {
        TestAccount account = createAccount();
        int initialGold = staticDataService.getAccountConst().initialGold();
        long battleId = enter(account, staticDataService.getFirstStageId());
        postAs(account, resultUrl(battleId), victoryResult()).andExpect(status().isOk());

        postAs(account, resultUrl(battleId), victoryResult())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BATTLE_ALREADY_COMPLETED"));

        getAs(account, SAVE_URL)
                .andExpect(jsonPath("$.currency.currencyGold").value(initialGold + GOLD));
    }

    @Test
    @DisplayName("다른 계정의 전투에 결과를 제출하면 403이고, 원래 주인은 그대로 제출할 수 있다")
    void submitOthersBattleFails() throws Exception {
        TestAccount owner = createAccount();
        TestAccount other = createAccount();
        long battleId = enter(owner, staticDataService.getFirstStageId());

        postAs(other, resultUrl(battleId), victoryResult())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_OWNED_BATTLE"));

        postAs(owner, resultUrl(battleId), victoryResult())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("스태미나를 다 쓰면 입장이 거부된다")
    void enterWithoutStaminaFails() throws Exception {
        TestAccount account = createAccount();
        AccountConstData accountConst = staticDataService.getAccountConst();
        int firstStageId = staticDataService.getFirstStageId();
        int possibleEnters = accountConst.initialStamina() / accountConst.battleStaminaCost();

        for (int i = 0; i < possibleEnters; i++) {
            enter(account, firstStageId);
        }

        postAs(account, ENTER_URL, Map.of("stageId", firstStageId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_ENOUGH_STAMINA"));
    }

    @Test
    @DisplayName("없는 스테이지에 입장하면 404")
    void enterUnknownStageFails() throws Exception {
        TestAccount account = createAccount();
        int unknownStageId = Collections.max(staticDataService.getStages().keySet()) + 1;

        postAs(account, ENTER_URL, Map.of("stageId", unknownStageId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STAGE_NOT_FOUND"));
    }

    private long enter(TestAccount account, int stageId) throws Exception {
        String body = bodyOf(postAs(account, ENTER_URL, Map.of("stageId", stageId))
                .andExpect(status().isOk()));
        Number battleId = JsonPath.read(body, "$.battleId");
        return battleId.longValue();
    }

    private String resultUrl(long battleId) {
        return ENTER_URL + "/" + battleId + "/result";
    }

    // 클라이언트처럼 모든 필드를 채워 보낸다 (업적 집계는 비워 둔다)
    private Map<String, Object> victoryResult() {
        Map<String, Object> request = new HashMap<>();
        request.put("victory", true);
        request.put("seconds", SECONDS);
        request.put("kills", KILLS);
        request.put("gold", GOLD);
        request.put("rewardBoxes", REWARD_BOXES);
        request.put("skillKills", Map.of());
        request.put("skillUpgrades", Map.of());
        request.put("pickups", Map.of());
        request.put("luckTrains", 0);
        return request;
    }
}