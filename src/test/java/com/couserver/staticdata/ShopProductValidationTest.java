package com.couserver.staticdata;

import com.couserver.inventory.entity.ItemGrade;
import com.couserver.staticdata.dto.ItemData;
import com.couserver.staticdata.dto.ProductType;
import com.couserver.staticdata.dto.ShopProductData;
import com.couserver.staticdata.service.StaticDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// "가격 0 행으로 기동 실패" 확인을, 기동 시 쓰는 검증 메서드를 직접 호출해 대신한다
class ShopProductValidationTest {
    private static final List<ItemData> NO_ITEMS = List.of();

    @Test
    @DisplayName("골드 상품의 가격이 0이면 기동을 멈춘다")
    void goldProductMustNotBeFree() {
        assertThatThrownBy(() -> StaticDataService.validateShopProduct(product(ProductType.GOLD, 0), NO_ITEMS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("priceGem");
    }

    @Test
    @DisplayName("랜덤 장비 상품의 가격이 0이면 기동을 멈춘다")
    void randomItemProductMustNotBeFree() {
        assertThatThrownBy(() -> StaticDataService.validateShopProduct(product(ProductType.RANDOMITEM, 0), NO_ITEMS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("priceGem");
    }

    @Test
    @DisplayName("보석 상품은 가격 0을 허용한다 (실제 결제 상품)")
    void gemProductMayHaveZeroPrice() {
        assertThatCode(() -> StaticDataService.validateShopProduct(product(ProductType.GEM, 0), NO_ITEMS))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("가격이 있는 골드 상품은 통과한다")
    void pricedGoldProductPasses() {
        assertThatCode(() -> StaticDataService.validateShopProduct(product(ProductType.GOLD, 30), NO_ITEMS))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("가격이 음수면 상품 종류와 관계없이 기동을 멈춘다")
    void negativePriceFails() {
        assertThatThrownBy(() -> StaticDataService.validateShopProduct(product(ProductType.GEM, -1), NO_ITEMS))
                .isInstanceOf(IllegalStateException.class);
    }

    private static ShopProductData product(ProductType type, int priceGem) {
        return new ShopProductData(99_999, type, priceGem, 100, ItemGrade.GENERAL, ItemGrade.GENERAL, true);
    }
}