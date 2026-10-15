package com.couserver.ranking;

import com.couserver.staticdata.service.StaticDataService;
import com.couserver.support.IntegrationTestSupport;
import com.couserver.support.TestAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 모든 테스트가 같은 Redis를 쓰므로, 상위 목록 구성 대신 "내 순위와 내 값"을 검증한다
class RankingApiTest extends IntegrationTestSupport {
    private static final String RANKING_URL = "/api/ranking";
    private static final int BATTLE_SECONDS = 3;

    @Autowired
    private StaticDataService staticDataService;

    @Test
    @DisplayName("신규 계정도 계정 레벨 랭킹에 레벨 1로 올라 있다")
    void accountLevelRanking() throws Exception {
        TestAccount account = createAccount();

        getAs(account, RANKING_URL + "?type=ACCOUNT_LEVEL")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ACCOUNT_LEVEL"))
                .andExpect(jsonPath("$.entries").isNotEmpty())
                .andExpect(jsonPath("$.myRank").isNumber())
                .andExpect(jsonPath("$.myValue").value(1));
    }

    @Test
    @DisplayName("스테이지를 클리어하면 최고 스테이지와 생존 시간 랭킹에 반영된다")
    void stageRankingsAfterClear() throws Exception {
        TestAccount account = createAccount();
        int firstStageId = staticDataService.getFirstStageId();
        clearStage(account, firstStageId, 0, BATTLE_SECONDS, Map.of());

        getAs(account, RANKING_URL + "?type=MAX_STAGE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRank").isNumber())
                .andExpect(jsonPath("$.myValue").value(firstStageId));

        getAs(account, RANKING_URL + "?type=STAGE_SURVIVAL&stageId=" + firstStageId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageId").value(firstStageId))
                .andExpect(jsonPath("$.myRank").isNumber())
                .andExpect(jsonPath("$.myValue").value(BATTLE_SECONDS));
    }

    @Test
    @DisplayName("잘못된 랭킹 종류는 400")
    void invalidRankingTypeFails() throws Exception {
        TestAccount account = createAccount();

        getAs(account, RANKING_URL + "?type=UNKNOWN")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RANKING_TYPE"));
    }

    @Test
    @DisplayName("없는 스테이지의 생존 랭킹은 404")
    void unknownStageSurvivalRankingFails() throws Exception {
        TestAccount account = createAccount();
        int unknownStageId = Collections.max(staticDataService.getStages().keySet()) + 1;

        getAs(account, RANKING_URL + "?type=STAGE_SURVIVAL&stageId=" + unknownStageId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STAGE_NOT_FOUND"));
    }
}