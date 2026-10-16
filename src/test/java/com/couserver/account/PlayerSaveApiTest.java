package com.couserver.account;

import com.couserver.staticdata.dto.AccountConstData;
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
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlayerSaveApiTest extends IntegrationTestSupport {
    @Autowired
    private StaticDataService staticDataService;

    @Test
    @DisplayName("신규 계정의 세이브는 정적 데이터의 초기값과 같다")
    void loadInitialSave() throws Exception {
        TestAccount account = createAccount();
        AccountConstData accountConst = staticDataService.getAccountConst();

        String body = bodyOf(getAs(account, SAVE_URL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.accountLevel").value(1))
                .andExpect(jsonPath("$.profile.accountExp").value(0))
                .andExpect(jsonPath("$.currency.currencyGold").value(accountConst.initialGold()))
                .andExpect(jsonPath("$.currency.currencyGem").value(accountConst.initialGem()))
                .andExpect(jsonPath("$.currency.currencyEnergy").value(accountConst.initialStamina()))
                .andExpect(jsonPath("$.stageProgress.currentStageId").value(staticDataService.getFirstStageId()))
                .andExpect(jsonPath("$.stageProgress.maxClearedStageId").value(nullValue()))
                .andExpect(jsonPath("$.playerStat.playerStatAttackLevel").value(0))
                .andExpect(jsonPath("$.playerStat.playerStatHpLevel").value(0))
                .andExpect(jsonPath("$.playerStat.playerStatDefenseLevel").value(0))
                .andExpect(jsonPath("$.playerStat.playerStatPotionRecoveryLevel").value(0))
                .andExpect(jsonPath("$.stageRecords").isEmpty()));

        Integer accountId = JsonPath.read(body, "$.profile.accountId");
        String nickname = JsonPath.read(body, "$.profile.playerNickname");
        assertThat(nickname).isEqualTo("플레이어" + accountId);
    }

    @Test
    @DisplayName("닉네임을 바꾸면 저장되고 다시 불러와도 유지된다")
    void changeNickname() throws Exception {
        TestAccount account = createAccount();
        String newNickname = "n" + randomSuffix(8);

        Map<String, Map<String, Object>> request = saveRequestFromCurrentSave(account);
        request.get("profile").put("playerNickname", newNickname);

        putAs(account, SAVE_URL, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.playerNickname").value(newNickname));

        getAs(account, SAVE_URL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.playerNickname").value(newNickname));
    }

    @Test
    @DisplayName("다른 계정이 쓰는 닉네임으로 바꾸면 409이고 내 닉네임은 그대로다")
    void changeToDuplicateNicknameFails() throws Exception {
        TestAccount owner = createAccount();
        TestAccount other = createAccount();
        String takenNickname = "n" + randomSuffix(8);

        Map<String, Map<String, Object>> ownerRequest = saveRequestFromCurrentSave(owner);
        ownerRequest.get("profile").put("playerNickname", takenNickname);
        putAs(owner, SAVE_URL, ownerRequest).andExpect(status().isOk());

        String nicknameBefore = currentNickname(other);
        Map<String, Map<String, Object>> otherRequest = saveRequestFromCurrentSave(other);
        otherRequest.get("profile").put("playerNickname", takenNickname);

        putAs(other, SAVE_URL, otherRequest)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        assertThat(currentNickname(other)).isEqualTo(nicknameBefore);
    }

    @Test
    @DisplayName("닉네임을 바꾸지 않고 저장해도 자기 닉네임을 중복으로 보지 않는다")
    void saveWithSameNicknameSucceeds() throws Exception {
        TestAccount account = createAccount();
        String nicknameBefore = currentNickname(account);

        putAs(account, SAVE_URL, saveRequestFromCurrentSave(account))
                .andExpect(status().isOk());

        assertThat(currentNickname(account)).isEqualTo(nicknameBefore);
    }

    @Test
    @DisplayName("해금된 첫 스테이지는 선택할 수 있다")
    void selectFirstStage() throws Exception {
        TestAccount account = createAccount();
        int firstStageId = staticDataService.getFirstStageId();

        Map<String, Map<String, Object>> request = saveRequestFromCurrentSave(account);
        request.get("stageProgress").put("currentStageId", firstStageId);

        putAs(account, SAVE_URL, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageProgress.currentStageId").value(firstStageId));
    }

    @Test
    @DisplayName("신규 계정이 해금되지 않은 스테이지를 선택하면 400이고 진행도는 그대로다")
    void selectLockedStageFails() throws Exception {
        TestAccount account = createAccount();
        int firstStageId = staticDataService.getFirstStageId();

        Map<String, Map<String, Object>> request = saveRequestFromCurrentSave(account);
        request.get("stageProgress").put("currentStageId", firstStageId + 1);

        putAs(account, SAVE_URL, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        getAs(account, SAVE_URL)
                .andExpect(jsonPath("$.stageProgress.currentStageId").value(firstStageId))
                .andExpect(jsonPath("$.stageProgress.maxClearedStageId").value(nullValue()));
    }


    private String currentNickname(TestAccount account) throws Exception {
        String body = bodyOf(getAs(account, SAVE_URL).andExpect(status().isOk()));
        return JsonPath.read(body, "$.profile.playerNickname");
    }
}