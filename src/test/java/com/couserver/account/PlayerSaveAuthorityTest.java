package com.couserver.account;

import com.couserver.staticdata.dto.EvolutionData;
import com.couserver.staticdata.service.StaticDataService;
import com.couserver.support.IntegrationTestSupport;
import com.couserver.support.TestAccount;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 1-a: save는 닉네임과 현재 스테이지만 반영하고 나머지 값은 무시한다 (설계안 1.6)
class PlayerSaveAuthorityTest extends IntegrationTestSupport {
    private static final int INFLATED = 999_999_999;

    @Autowired
    private StaticDataService staticDataService;

    @Test
    @DisplayName("save로 보낸 gold와 gem은 반영되지 않는다 (F1)")
    void currencyIsIgnored() throws Exception {
        TestAccount account = createAccount();
        setGoldAndGem(account, 100, 10);

        Map<String, Map<String, Object>> request = saveRequestFromCurrentSave(account);
        request.get("currency").put("currencyGold", INFLATED);
        request.get("currency").put("currencyGem", INFLATED);

        putAs(account, SAVE_URL, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency.currencyGold").value(100))
                .andExpect(jsonPath("$.currency.currencyGem").value(10));

        getAs(account, SAVE_URL)
                .andExpect(jsonPath("$.currency.currencyGold").value(100))
                .andExpect(jsonPath("$.currency.currencyGem").value(10));
    }

    @Test
    @DisplayName("save로 보낸 스태미나와 회복 기준 시각은 반영되지 않는다 (F1)")
    void energyIsIgnored() throws Exception {
        TestAccount account = createAccount();
        // 스태미나를 최대치 아래로 내려 둔다. 최대치에서는 조회할 때마다 회복 기준 시각이 현재로 바뀌어 비교할 수 없다
        postAs(account, "/api/battles", Map.of("stageId", staticDataService.getFirstStageId()))
                .andExpect(status().isOk());

        String before = saveOf(account);
        Object energyBefore = JsonPath.read(before, "$.currency.currencyEnergy");
        Object updatedAtBefore = JsonPath.read(before, "$.currency.currencyEnergyUpdatedAt");

        Map<String, Map<String, Object>> request = saveRequestFromCurrentSave(account);
        request.get("currency").put("currencyEnergy", 9999);
        request.get("currency").put("currencyEnergyUpdatedAt", "2000-01-01T00:00:00Z");
        putAs(account, SAVE_URL, request).andExpect(status().isOk());

        String after = saveOf(account);
        assertThat((Object) JsonPath.read(after, "$.currency.currencyEnergy")).isEqualTo(energyBefore);
        assertThat((Object) JsonPath.read(after, "$.currency.currencyEnergyUpdatedAt")).isEqualTo(updatedAtBefore);
    }

    @Test
    @DisplayName("save로 보낸 진화 스탯은 반영되지 않고 전투력 랭킹도 그대로다 (F3)")
    void playerStatIsIgnored() throws Exception {
        TestAccount account = createAccount();
        Object combatPowerBefore = JsonPath.read(
                bodyOf(getAs(account, "/api/ranking?type=COMBAT_POWER").andExpect(status().isOk())), "$.myValue");

        Map<String, Map<String, Object>> request = saveRequestFromCurrentSave(account);
        request.get("playerStat").put("playerStatAttackLevel", 100_000);
        request.get("playerStat").put("playerStatHpLevel", 100_000);
        request.get("playerStat").put("playerStatDefenseLevel", 100_000);

        putAs(account, SAVE_URL, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerStat.playerStatAttackLevel").value(0))
                .andExpect(jsonPath("$.playerStat.playerStatHpLevel").value(0))
                .andExpect(jsonPath("$.playerStat.playerStatDefenseLevel").value(0));

        Object combatPowerAfter = JsonPath.read(
                bodyOf(getAs(account, "/api/ranking?type=COMBAT_POWER").andExpect(status().isOk())), "$.myValue");
        assertThat(combatPowerAfter).isEqualTo(combatPowerBefore);
    }

    @Test
    @DisplayName("save로 레벨을 올려도 반영되지 않고 진화는 여전히 레벨 제한에 걸린다 (F6)")
    void accountLevelIsIgnored() throws Exception {
        TestAccount account = createAccount();

        Map<String, Map<String, Object>> request = saveRequestFromCurrentSave(account);
        request.get("profile").put("accountLevel", 999);
        putAs(account, SAVE_URL, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.accountLevel").value(1));

        EvolutionData evolution = staticDataService.getEvolution();
        int allowedSteps = evolution.levelStepLimit(1);
        int totalCost = 0;
        for (int step = 0; step <= allowedSteps; step++) {   // 레벨 제한이 없다면 한 번 더 진화할 수 있는 골드까지 준다
            totalCost += evolution.costAt(step);
        }
        setGoldAndGem(account, totalCost, 0);

        for (int step = 0; step < allowedSteps; step++) {
            postAs(account, "/api/players/me/evolution/upgrade").andExpect(status().isOk());
        }
        postAs(account, "/api/players/me/evolution/upgrade")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EVOLUTION_LEVEL_LOCKED"));
    }

    @Test
    @DisplayName("save로 보낸 최고 클리어 스테이지는 반영되지 않고 랭킹에도 오르지 않는다 (F5)")
    void maxClearedStageIsIgnored() throws Exception {
        TestAccount account = createAccount();

        Map<String, Map<String, Object>> request = saveRequestFromCurrentSave(account);
        request.get("stageProgress").put("maxClearedStageId", 999_999);
        putAs(account, SAVE_URL, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageProgress.maxClearedStageId").value(nullValue()));

        getAs(account, "/api/ranking?type=MAX_STAGE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRank").value(nullValue()));
    }

    @Test
    @DisplayName("스테이지 해금은 서버에 저장된 진행도로 판정한다 (F8)")
    void stageUnlockUsesServerProgress() throws Exception {
        TestAccount account = createAccount();
        int firstStageId = staticDataService.getFirstStageId();

        Map<String, Map<String, Object>> request = saveRequestFromCurrentSave(account);
        request.get("stageProgress").put("currentStageId", 5);
        request.get("stageProgress").put("maxClearedStageId", 4);

        putAs(account, SAVE_URL, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        getAs(account, SAVE_URL)
                .andExpect(jsonPath("$.stageProgress.currentStageId").value(firstStageId))
                .andExpect(jsonPath("$.stageProgress.maxClearedStageId").value(nullValue()));
    }

    @Test
    @DisplayName("클리어한 스테이지는 선택할 수 있고, 정적 데이터에 없는 스테이지는 선택할 수 없다")
    void selectStageAfterClear() throws Exception {
        TestAccount account = createAccount();
        int firstStageId = staticDataService.getFirstStageId();
        clearStage(account, firstStageId, 0, 3, Map.of());

        Map<String, Map<String, Object>> request = saveRequestFromCurrentSave(account);
        request.get("stageProgress").put("currentStageId", firstStageId);
        putAs(account, SAVE_URL, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageProgress.currentStageId").value(firstStageId))
                .andExpect(jsonPath("$.stageProgress.maxClearedStageId").value(firstStageId));

        int unknownStageId = Collections.max(staticDataService.getStages().keySet()) + 1;
        request.get("stageProgress").put("currentStageId", unknownStageId);
        putAs(account, SAVE_URL, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        getAs(account, SAVE_URL)
                .andExpect(jsonPath("$.stageProgress.currentStageId").value(firstStageId));
    }

    @Test
    @DisplayName("무시하는 필드를 모두 생략한 요청도 받아들인다 (새 클라이언트 호환)")
    void omittedIgnoredFieldsAreAccepted() throws Exception {
        TestAccount account = createAccount();
        String newNickname = "n" + randomSuffix(8);
        Map<String, Object> request = Map.of(
                "profile", Map.of("playerNickname", newNickname),
                "stageProgress", Map.of("currentStageId", staticDataService.getFirstStageId()));

        putAs(account, SAVE_URL, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.playerNickname").value(newNickname))
                .andExpect(jsonPath("$.profile.accountLevel").value(1));
    }
}