package com.couserver.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 통합 테스트 공통 베이스. 모든 통합 테스트가 같은 설정을 써야 컨텍스트와 컨테이너가 재사용된다.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestSupport {

    // 2단계 비밀번호 정책(8자 이상, 72바이트 이하)을 미리 만족하는 값
    protected static final String TEST_PASSWORD = "test-password-1234";
    protected static final String SAVE_URL = "/api/players/me/save";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    // 테스트끼리 겹치지 않는 loginId. ^[a-z0-9_]{3,20}$ 규칙에 맞는 12자
    protected String uniqueLoginId() {
        return "t_" + randomSuffix(10);
    }

    // 소문자 16진수 문자열
    protected String randomSuffix(int length) {
        return UUID.randomUUID().toString().replace("-", "").substring(0, length);
    }

    protected ResultActions signup(String loginId, String password) throws Exception {
        return mockMvc.perform(post("/api/accounts/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("accountLoginId", loginId, "password", password))));
    }

    protected ResultActions login(String loginId, String password) throws Exception {
        return mockMvc.perform(post("/api/accounts/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("accountLoginId", loginId, "password", password))));
    }

    // 새 계정을 가입시키고 로그인해서 토큰과 playerId를 받아 온다
    protected TestAccount createAccount() throws Exception {
        String loginId = uniqueLoginId();
        signup(loginId, TEST_PASSWORD).andExpect(status().isCreated());
        String loginBody = bodyOf(login(loginId, TEST_PASSWORD).andExpect(status().isOk()));
        String accessToken = JsonPath.read(loginBody, "$.accessToken");

        String saveBody = bodyOf(mockMvc.perform(get(SAVE_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk()));
        Number playerId = JsonPath.read(saveBody, "$.profile.playerId");
        return new TestAccount(loginId, accessToken, playerId.longValue());
    }

    protected ResultActions getAs(TestAccount account, String url) throws Exception {
        return mockMvc.perform(get(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + account.accessToken()));
    }

    protected ResultActions putAs(TestAccount account, String url, Object body) throws Exception {
        return mockMvc.perform(put(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + account.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(body)));
    }

    protected ResultActions postAs(TestAccount account, String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + account.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(body)));
    }

    // 본문 없는 POST (진화 등)
    protected ResultActions postAs(TestAccount account, String url) throws Exception {
        return mockMvc.perform(post(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + account.accessToken()));
    }

    protected ResultActions deleteAs(TestAccount account, String url) throws Exception {
        return mockMvc.perform(delete(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + account.accessToken()));
    }

    // 테스트 준비용: 스테이지에 입장해 승리 결과를 제출한다 (골드·보상상자 0). 결과 응답 본문을 돌려준다.
    // seconds는 "입장 후 경과 시간 + 5초"까지 인정되므로 5 이하로 쓴다.
    protected String clearStage(TestAccount account, int stageId, int kills, int seconds,
                                Map<Integer, Integer> skillKills) throws Exception {
        String enterBody = bodyOf(postAs(account, "/api/battles", Map.of("stageId", stageId))
                .andExpect(status().isOk()));
        Number battleId = JsonPath.read(enterBody, "$.battleId");

        Map<String, Object> result = new HashMap<>();
        result.put("victory", true);
        result.put("seconds", seconds);
        result.put("kills", kills);
        result.put("gold", 0);
        result.put("rewardBoxes", 0);
        result.put("skillKills", skillKills);
        result.put("skillUpgrades", Map.of());
        result.put("pickups", Map.of());
        result.put("luckTrains", 0);
        return bodyOf(postAs(account, "/api/battles/" + battleId.longValue() + "/result", result)
                .andExpect(status().isOk()));
    }

    // 현재 세이브 응답 본문
    protected String saveOf(TestAccount account) throws Exception {
        return bodyOf(getAs(account, SAVE_URL).andExpect(status().isOk()));
    }

    // 테스트 준비용: gold와 gem을 DB에 직접 넣는다.
    // Currency.update()는 1-a에서 삭제될 예정이라 쓰지 않는다.
    protected void setGoldAndGem(TestAccount account, int gold, int gem) {
        int updated = jdbcTemplate.update(
                "UPDATE currency SET currency_gold = ?, currency_gem = ? WHERE player_id = ?",
                gold, gem, account.playerId());
        assertThat(updated).as("currency 행이 하나 갱신되어야 한다").isEqualTo(1);
    }

    // 한글이 깨지지 않도록 응답 본문을 UTF-8로 읽는다
    protected String bodyOf(ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    protected String toJson(Object value) {
        return objectMapper.writeValueAsString(value);
    }
}