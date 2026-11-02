package com.couserver.social;

import com.couserver.support.IntegrationTestSupport;
import com.couserver.support.TestAccount;
import com.couserver.support.TestFirebaseTokens;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 구글 로그인·연동 API. 토큰은 테스트 키로 서명한 Firebase 형식 토큰이다 (TestFirebaseTokens)
class SocialLoginApiTest extends IntegrationTestSupport {
    private static final String LOGIN_URL = "/api/accounts/google/login";
    private static final String LINK_URL = "/api/accounts/me/google";

    @Test
    @DisplayName("처음 보는 구글 사용자는 새 계정이 만들어지고, 그 토큰으로 API를 쓸 수 있다")
    void newUserCreatesAccount() throws Exception {
        String uid = newUid();

        String body = bodyOf(googleLogin(TestFirebaseTokens.idToken(uid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newAccount").value(true))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.account.accountLoginId").value(org.hamcrest.Matchers.startsWith("s:"))));

        TestAccount account = fromLogin(body);
        getAs(account, SAVE_URL).andExpect(status().isOk());   // 초기 데이터가 만들어졌고 토큰이 동작한다
        getAs(account, LINK_URL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(true));
    }

    @Test
    @DisplayName("이미 연동된 구글 사용자는 같은 계정으로 로그인한다")
    void linkedUserLogsIntoSameAccount() throws Exception {
        String uid = newUid();
        int first = accountIdOf(bodyOf(googleLogin(TestFirebaseTokens.idToken(uid)).andExpect(status().isOk())));

        String second = bodyOf(googleLogin(TestFirebaseTokens.idToken(uid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newAccount").value(false)));
        assertThat(accountIdOf(second)).isEqualTo(first);
    }

    @Test
    @DisplayName("기존 계정에 연동하면, 이후 구글 로그인이 그 계정으로 들어간다")
    void linkExistingAccount() throws Exception {
        TestAccount account = createAccount();
        String uid = newUid();

        postAs(account, LINK_URL, Map.of("idToken", TestFirebaseTokens.idToken(uid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(true))
                .andExpect(jsonPath("$.linkedAt").exists());

        googleLogin(TestFirebaseTokens.idToken(uid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newAccount").value(false))
                .andExpect(jsonPath("$.account.accountLoginId").value(account.loginId()));
    }

    @Test
    @DisplayName("같은 신원으로 다시 연동하면 그대로 성공한다 (중복 요청에 안전)")
    void linkSameIdentityAgain() throws Exception {
        TestAccount account = createAccount();
        String token = TestFirebaseTokens.idToken(newUid());

        postAs(account, LINK_URL, Map.of("idToken", token)).andExpect(status().isOk());
        postAs(account, LINK_URL, Map.of("idToken", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(true));
    }

    @Test
    @DisplayName("다른 계정에 연동된 구글 신원으로 연동하면 409 SOCIAL_IDENTITY_ALREADY_LINKED")
    void identityLinkedToOtherAccount() throws Exception {
        String token = TestFirebaseTokens.idToken(newUid());
        postAs(createAccount(), LINK_URL, Map.of("idToken", token)).andExpect(status().isOk());

        postAs(createAccount(), LINK_URL, Map.of("idToken", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SOCIAL_IDENTITY_ALREADY_LINKED"));
    }

    @Test
    @DisplayName("이미 구글이 연동된 계정에 다른 구글 신원을 연동하면 409 SOCIAL_PROVIDER_ALREADY_LINKED")
    void accountAlreadyHasProvider() throws Exception {
        TestAccount account = createAccount();
        postAs(account, LINK_URL, Map.of("idToken", TestFirebaseTokens.idToken(newUid()))).andExpect(status().isOk());

        postAs(account, LINK_URL, Map.of("idToken", TestFirebaseTokens.idToken(newUid())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SOCIAL_PROVIDER_ALREADY_LINKED"));
    }

    @Test
    @DisplayName("다른 프로젝트의 토큰은 401 SOCIAL_TOKEN_INVALID이고 계정이 만들어지지 않는다")
    void otherProjectTokenRejected() throws Exception {
        String uid = newUid();
        googleLogin(TestFirebaseTokens.idToken(uid, "other-project"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SOCIAL_TOKEN_INVALID"));

        assertThat(linkCount(uid)).isZero();
    }

    @Test
    @DisplayName("처음 보는 구글 사용자가 동시에 두 번 로그인해도 계정은 하나만 만들어진다")
    void concurrentFirstLogin() throws Exception {
        String token = TestFirebaseTokens.idToken(newUid());
        ConcurrentLinkedQueue<String> bodies = new ConcurrentLinkedQueue<>();

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            tasks.add(() -> {
                var result = googleLogin(token).andReturn().getResponse();
                bodies.add(result.getContentAsString());
                return result.getStatus();
            });
        }

        assertThat(runConcurrently(tasks)).containsExactly(200, 200);
        List<Integer> accountIds = bodies.stream().map(SocialLoginApiTest::accountIdOf).toList();
        assertThat(accountIds.get(0)).isEqualTo(accountIds.get(1));
        List<Boolean> newFlags = bodies.stream().map(b -> (Boolean) JsonPath.read(b, "$.newAccount")).toList();
        assertThat(newFlags).containsExactlyInAnyOrder(true, false);
    }

    @Test
    @DisplayName("같은 계정에 같은 신원으로 동시에 두 번 연동해도 한 줄만 저장된다")
    void concurrentLink() throws Exception {
        TestAccount account = createAccount();
        String uid = newUid();
        String token = TestFirebaseTokens.idToken(uid);

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            tasks.add(() -> postAs(account, LINK_URL, Map.of("idToken", token)).andReturn().getResponse().getStatus());
        }

        assertThat(runConcurrently(tasks)).containsExactly(200, 200);
        assertThat(linkCount(uid)).isEqualTo(1);
    }

    private ResultActions googleLogin(String idToken) throws Exception {
        return mockMvc.perform(post(LOGIN_URL)
                .with(fromIp(randomIp()))   // IP 요청 제한에 테스트끼리 걸리지 않게
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("idToken", idToken))));
    }

    private String newUid() {
        return "test-uid-" + randomSuffix(12);
    }

    private int linkCount(String uid) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM social_link WHERE social_link_provider_user_id = ?", Integer.class, uid);
    }

    private static int accountIdOf(String loginBody) {
        return JsonPath.read(loginBody, "$.account.accountId");
    }

    private static TestAccount fromLogin(String loginBody) {
        return new TestAccount(JsonPath.read(loginBody, "$.account.accountLoginId"),
                JsonPath.read(loginBody, "$.accessToken"), null);
    }
}