package com.couserver.ranking;

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
    @DisplayName("스테이지를 클리어하면 최고 스테이지와 클리어 시간 랭킹에 반영된다")
    void stageRankingsAfterClear() throws Exception {
        TestAccount account = createAccount();
        int firstStageId = staticDataService.getFirstStageId();
        clearStage(account, firstStageId, 0, BATTLE_SECONDS, Map.of());

        getAs(account, RANKING_URL + "?type=MAX_STAGE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRank").isNumber())
                .andExpect(jsonPath("$.myValue").value(firstStageId));

        getAs(account, RANKING_URL + "?type=STAGE_CLEAR&stageId=" + firstStageId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageId").value(firstStageId))
                .andExpect(jsonPath("$.myRank").isNumber())
                .andExpect(jsonPath("$.myValue").value(BATTLE_SECONDS));
    }

    @Test
    @DisplayName("클리어 시간 랭킹은 더 빨리 클리어한 계정이 위에 있다")
    void stageClearRankingOrdersByShortestTime() throws Exception {
        TestAccount fast = createAccount();
        TestAccount slow = createAccount();
        int firstStageId = staticDataService.getFirstStageId();
        clearStage(fast, firstStageId, 0, BATTLE_SECONDS, Map.of());
        clearStage(slow, firstStageId, 0, BATTLE_SECONDS + 10, Map.of());

        String url = RANKING_URL + "?type=STAGE_CLEAR&stageId=" + firstStageId;
        Integer fastRank = JsonPath.read(bodyOf(getAs(fast, url)), "$.myRank");
        Integer slowRank = JsonPath.read(bodyOf(getAs(slow, url)), "$.myRank");
        assertThat(fastRank).isLessThan(slowRank);
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
    @DisplayName("없는 스테이지의 클리어 시간 랭킹은 404")
    void unknownStageClearRankingFails() throws Exception {
        TestAccount account = createAccount();
        int unknownStageId = Collections.max(staticDataService.getStages().keySet()) + 1;

        getAs(account, RANKING_URL + "?type=STAGE_CLEAR&stageId=" + unknownStageId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STAGE_NOT_FOUND"));
    }
}