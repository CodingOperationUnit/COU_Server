package com.couserver.shop;

import com.couserver.inventory.entity.ItemGrade;
import com.couserver.staticdata.dto.ProductType;
import com.couserver.staticdata.dto.ShopProductData;
import com.couserver.staticdata.service.StaticDataService;
import com.couserver.support.IntegrationTestSupport;
import com.couserver.support.TestAccount;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ShopApiTest extends IntegrationTestSupport {
    private static final String PRODUCTS_URL = "/api/shop/products";
    private static final String PURCHASE_URL = "/api/shop/purchase";
    private static final int GOLD_PACK = 50011;
    private static final int RANDOM_ITEM_GENERAL_TO_SUPER = 50001;
    private static final int RANDOM_ITEM_SUPER_TO_RARE = 50002;

    @Autowired
    private StaticDataService staticDataService;

    @Test
    @DisplayName("상품 목록에 골드 팩과 랜덤 장비 상품이 있다")
    void listProducts() throws Exception {
        TestAccount account = createAccount();

        getAs(account, PRODUCTS_URL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].productId",
                        hasItems(GOLD_PACK, RANDOM_ITEM_GENERAL_TO_SUPER, RANDOM_ITEM_SUPER_TO_RARE)));
    }

    @Test
    @DisplayName("골드 팩을 사면 gem이 차감되고 gold가 지급된다")
    void buyGoldPack() throws Exception {
        TestAccount account = createAccount();
        ShopProductData product = staticDataService.getShopProducts().get(GOLD_PACK);
        setGoldAndGem(account, 0, product.priceGem());

        postAs(account, PURCHASE_URL, Map.of("productId", GOLD_PACK))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spentGem").value(product.priceGem()))
                .andExpect(jsonPath("$.currencyGem").value(0))
                .andExpect(jsonPath("$.currencyGold").value(product.rewardAmount()))
                .andExpect(jsonPath("$.rewardedItems.length()").value(0));

        getAs(account, SAVE_URL)
                .andExpect(jsonPath("$.currency.currencyGem").value(0))
                .andExpect(jsonPath("$.currency.currencyGold").value(product.rewardAmount()));
    }

    @ParameterizedTest(name = "상품 {0}")
    @ValueSource(ints = {RANDOM_ITEM_GENERAL_TO_SUPER, RANDOM_ITEM_SUPER_TO_RARE})
    @DisplayName("랜덤 장비를 사면 등급 범위 안의 장비 1개가 인벤토리에 들어온다")
    void buyRandomItem(int productId) throws Exception {
        TestAccount account = createAccount();
        ShopProductData product = staticDataService.getShopProducts().get(productId);
        setGoldAndGem(account, 0, product.priceGem());

        String body = bodyOf(postAs(account, PURCHASE_URL, Map.of("productId", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currencyGem").value(0))
                .andExpect(jsonPath("$.rewardedItems.length()").value(1)));

        String gradeName = JsonPath.read(body, "$.rewardedItems[0].inventoryItemGrade");
        ItemGrade grade = ItemGrade.valueOf(gradeName.toUpperCase(Locale.ROOT));
        assertThat(grade.ordinal())
                .isBetween(product.minGrade().ordinal(), product.maxGrade().ordinal());

        Number inventoryId = JsonPath.read(body, "$.rewardedItems[0].inventoryId");
        getAs(account, "/api/inventory")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].inventoryId", hasItem(inventoryId.intValue())));
    }

    @Test
    @DisplayName("gem이 부족하면 400이고 재화는 그대로다")
    void buyWithoutEnoughGemFails() throws Exception {
        TestAccount account = createAccount();
        ShopProductData product = staticDataService.getShopProducts().get(GOLD_PACK);
        setGoldAndGem(account, 0, product.priceGem() - 1);

        postAs(account, PURCHASE_URL, Map.of("productId", GOLD_PACK))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_ENOUGH_CURRENCY"));

        getAs(account, SAVE_URL)
                .andExpect(jsonPath("$.currency.currencyGem").value(product.priceGem() - 1))
                .andExpect(jsonPath("$.currency.currencyGold").value(0));
    }

    @Test
    @DisplayName("없는 상품을 사면 404")
    void buyUnknownProductFails() throws Exception {
        TestAccount account = createAccount();
        int unknownProductId = Collections.max(staticDataService.getShopProducts().keySet()) + 1;

        postAs(account, PURCHASE_URL, Map.of("productId", unknownProductId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("보석 상품은 결제 검증 없이 구매할 수 없고 보석은 그대로다 (F2)")
    void buyGemProductFails() throws Exception {
        TestAccount account = createAccount();
        setGoldAndGem(account, 0, 0);
        List<ShopProductData> gemProducts = staticDataService.getShopProducts().values().stream()
                .filter(product -> product.productType() == ProductType.GEM && product.active())
                .toList();
        assertThat(gemProducts).as("판매 중인 보석 상품이 있어야 한다").isNotEmpty();

        for (ShopProductData product : gemProducts) {
            postAs(account, PURCHASE_URL, Map.of("productId", product.productId()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("PAYMENT_VERIFICATION_REQUIRED"));
        }

        getAs(account, SAVE_URL)
                .andExpect(jsonPath("$.currency.currencyGem").value(0));
    }

    @Test
    @DisplayName("상품 목록은 정적 데이터의 판매 중인 상품과 같고 보석 상품도 그대로 보인다")
    void productListMatchesStaticData() throws Exception {
        TestAccount account = createAccount();
        Object[] activeIds = staticDataService.getShopProducts().values().stream()
                .filter(ShopProductData::active)
                .map(ShopProductData::productId)
                .toArray();

        getAs(account, PRODUCTS_URL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(activeIds.length))
                .andExpect(jsonPath("$[*].productId", containsInAnyOrder(activeIds)));
    }
}