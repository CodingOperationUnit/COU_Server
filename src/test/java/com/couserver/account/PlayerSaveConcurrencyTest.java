package com.couserver.account;

import com.couserver.staticdata.dto.ShopProductData;
import com.couserver.staticdata.service.StaticDataService;
import com.couserver.support.IntegrationTestSupport;
import com.couserver.support.TestAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class PlayerSaveConcurrencyTest extends IntegrationTestSupport {
    private static final int REPEAT = 20;
    private static final int GOLD_PACK = 50011;

    @Autowired
    private StaticDataService staticDataService;

    @Test
    @DisplayName("save와 상점 구매를 동시에 보내도 구매 결과가 사라지지 않는다")
    void saveDoesNotOverwritePurchases() throws Exception {
        TestAccount account = createAccount();
        ShopProductData product = staticDataService.getShopProducts().get(GOLD_PACK);
        setGoldAndGem(account, 0, product.priceGem() * REPEAT);

        // 구매 전 재화가 담긴 요청. 수정 전에는 이 요청이 구매 결과를 덮어쓴다
        Map<String, Map<String, Object>> staleSave = saveRequestFromCurrentSave(account);

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < REPEAT; i++) {
            tasks.add(() -> postAs(account, "/api/shop/purchase", Map.of("productId", GOLD_PACK))
                    .andReturn().getResponse().getStatus());
            tasks.add(() -> putAs(account, SAVE_URL, staleSave)
                    .andReturn().getResponse().getStatus());
        }

        List<Integer> statuses = runConcurrently(tasks);

        assertThat(statuses).containsOnly(200);
        getAs(account, SAVE_URL)
                .andExpect(jsonPath("$.currency.currencyGem").value(0))
                .andExpect(jsonPath("$.currency.currencyGold").value(product.rewardAmount() * REPEAT));
    }

    @Test
    @DisplayName("두 계정이 같은 닉네임으로 동시에 바꾸면 하나만 성공하고 나머지는 409다")
    void concurrentNicknameChange() throws Exception {
        int pairs = 10;
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < pairs; i++) {
            String nickname = "n" + randomSuffix(8);
            for (TestAccount account : List.of(createAccount(), createAccount())) {
                Map<String, Map<String, Object>> request = saveRequestFromCurrentSave(account);
                request.get("profile").put("playerNickname", nickname);
                tasks.add(() -> putAs(account, SAVE_URL, request).andReturn().getResponse().getStatus());
            }
        }

        List<Integer> statuses = runConcurrently(tasks);

        for (int i = 0; i < pairs; i++) {
            assertThat(statuses.subList(i * 2, i * 2 + 2))
                    .as("%d번째 쌍", i)
                    .containsExactlyInAnyOrder(200, 409);
        }
    }
}