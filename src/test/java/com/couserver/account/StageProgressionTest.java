package com.couserver.account;

import com.couserver.staticdata.service.StaticDataService;
import com.couserver.support.IntegrationTestSupport;
import com.couserver.support.TestAccount;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 여러 스테이지가 있는 정적 데이터로 해금 규칙을 실제 API에서 확인한다 (StageUnlockPolicyTest의 통합 버전)
class StageProgressionTest extends IntegrationTestSupport {

    @Autowired
    private StaticDataService staticDataService;

    private int first;
    private int second;
    private int third;

    @BeforeEach
    void setUp() {
        List<Integer> stageIds = staticDataService.getStages().keySet().stream().sorted().toList();
        assumeTrue(stageIds.size() >= 3, "스테이지가 3개 이상 있어야 하는 테스트");
        first = stageIds.get(0);
        second = stageIds.get(1);
        third = stageIds.get(2);
    }

    @Test
    @DisplayName("첫 스테이지를 클리어하면 다음 스테이지를 고를 수 있고, 그다음 스테이지는 아직 고를 수 없다")
    void selectNextStageAfterClear() throws Exception {
        TestAccount account = createAccount();
        clearStage(account, first, 0, 3, Map.of());

        selectStage(account, second)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageProgress.currentStageId").value(second));

        selectStage(account, third)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        getAs(account, SAVE_URL)
                .andExpect(jsonPath("$.stageProgress.currentStageId").value(second))
                .andExpect(jsonPath("$.stageProgress.maxClearedStageId").value(first));
    }

    @Test
    @DisplayName("패배로 끝난 전투는 다음 스테이지를 열지 않는다")
    void defeatDoesNotUnlockNextStage() throws Exception {
        TestAccount account = createAccount();
        finishBattle(account, first, false);

        selectStage(account, second)
                .andExpect(status().isBadRequest());

        getAs(account, SAVE_URL)
                .andExpect(jsonPath("$.stageProgress.maxClearedStageId").value(nullValue()));
    }

    @Test
    @DisplayName("전투 입장도 save와 같은 해금 규칙을 따른다")
    void battleEntryFollowsSameRule() throws Exception {
        TestAccount account = createAccount();

        enter(account, second)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STAGE_LOCKED"));

        clearStage(account, first, 0, 3, Map.of());

        enter(account, second).andExpect(status().isOk());
        enter(account, third)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STAGE_LOCKED"));
    }

    @Test
    @DisplayName("스테이지를 차례로 클리어하면 그다음 스테이지가 열린다")
    void clearingInOrderUnlocksTheNext() throws Exception {
        TestAccount account = createAccount();
        clearStage(account, first, 0, 3, Map.of());
        clearStage(account, second, 0, 3, Map.of());

        selectStage(account, third)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageProgress.currentStageId").value(third))
                .andExpect(jsonPath("$.stageProgress.maxClearedStageId").value(second));
    }

    // 현재 세이브를 그대로 보내고 현재 스테이지만 바꾼다
    private ResultActions selectStage(TestAccount account, int stageId) throws Exception {
        Map<String, Map<String, Object>> request = saveRequestFromCurrentSave(account);
        request.get("stageProgress").put("currentStageId", stageId);
        return putAs(account, SAVE_URL, request);
    }

    private ResultActions enter(TestAccount account, int stageId) throws Exception {
        return postAs(account, "/api/battles", Map.of("stageId", stageId));
    }

    private void finishBattle(TestAccount account, int stageId, boolean victory) throws Exception {
        String enterBody = bodyOf(enter(account, stageId).andExpect(status().isOk()));
        Number battleId = JsonPath.read(enterBody, "$.battleId");

        Map<String, Object> result = new HashMap<>();
        result.put("victory", victory);
        result.put("seconds", 3);
        result.put("kills", 0);
        result.put("gold", 0);
        result.put("rewardBoxes", 0);
        result.put("quality", 0);
        result.put("skillKills", Map.of());
        result.put("skillUpgrades", Map.of());
        result.put("pickups", Map.of());
        result.put("luckTrains", 0);

        postAs(account, "/api/battles/" + battleId.longValue() + "/result", result)
                .andExpect(status().isOk());
    }
}