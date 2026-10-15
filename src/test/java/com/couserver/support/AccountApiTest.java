package com.couserver.support;

import com.couserver.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountApiTest extends IntegrationTestSupport{
    @Test
    @DisplayName("회원가입에 성공하면 201과 계정 정보를 돌려준다.")
    void signUpSucceeds() throws Exception{
        String loginId = uniqueLoginId();

        signup(loginId, TEST_PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").isNumber())
                .andExpect(jsonPath("$.accountLoginId").value(loginId));
    }

    @Test
    @DisplayName("이미 있는 아이디로 가입하면 409")
    void signupWithDuplicateLoginIdFails() throws Exception {
        String loginId = uniqueLoginId();
        signup(loginId, TEST_PASSWORD).andExpect(status().isCreated());

        signup(loginId, TEST_PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    @DisplayName("아이디 형식이 틀리면 400")
    void signupWithInvalidLoginIdFails() throws Exception {
        signup("T_Upper" + randomSuffix(4), TEST_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("로그인에 성공하면 Bearer 토큰을 돌려준다")
    void loginSucceeds() throws Exception {
        String loginId = uniqueLoginId();
        signup(loginId, TEST_PASSWORD).andExpect(status().isCreated());

        login(loginId, TEST_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.account.accountLoginId").value(loginId));
    }

    @Test
    @DisplayName("비밀번호가 틀리면 401")
    void loginWithWrongPasswordFails() throws Exception {
        String loginId = uniqueLoginId();
        signup(loginId, TEST_PASSWORD).andExpect(status().isCreated());

        login(loginId, TEST_PASSWORD + "x")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("없는 아이디로 로그인하면 401")
    void loginWithUnknownLoginIdFails() throws Exception {
        login(uniqueLoginId(), TEST_PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("토큰 없이 인증이 필요한 API를 호출하면 401")
    void requestWithoutTokenFails() throws Exception {
        mockMvc.perform(get("/api/players/me/save"))
                .andExpect(status().isUnauthorized());
    }
}
