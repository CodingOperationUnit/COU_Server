package com.couserver.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public class IntegrationTestSupport {
    protected static final String TEST_PASSWORD = "test-password-1234";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // 테스트끼리 겹치지 않는 loginId.
    protected String uniqueLoginId(){
        return "t_" + randomSuffix(10);
    }

    // 소문자 16진수 문자열
    protected String randomSuffix(int length){
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

    // 새 계정을 가입시키고 로그인해서 토큰을 받아 온다
    protected TestAccount createAccount() throws Exception {
        String loginId = uniqueLoginId();
        signup(loginId, TEST_PASSWORD).andExpect(status().isCreated());
        String body = bodyOf(login(loginId, TEST_PASSWORD).andExpect(status().isOk()));
        String accessToken = JsonPath.read(body, "$.accessToken");
        return new TestAccount(loginId, accessToken);
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

    // 한글이 깨지지 않도록 응답 본문을 UTF-8로 읽는다
    protected String bodyOf(ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    protected String toJson(Object value) {
        return objectMapper.writeValueAsString(value);
    }

}
