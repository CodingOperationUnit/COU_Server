package com.couserver.shop;

import com.couserver.account.entity.Currency;
import com.couserver.account.repository.CurrencyRepository;
import com.couserver.common.exception.BusinessException;
import com.couserver.inventory.entity.ItemGrade;
import com.couserver.inventory.repository.EquipmentRepository;
import com.couserver.shop.exception.ShopErrorCode;
import com.couserver.shop.service.ShopService;
import com.couserver.staticdata.dto.ProductType;
import com.couserver.staticdata.dto.ShopProductData;
import com.couserver.staticdata.service.StaticDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShopServiceGemGuardTest {
    private static final long PLAYER_ID = 1L;
    private static final int GEM_PRODUCT_ID = 50021;
    private static final ShopProductData GEM_PRODUCT = new ShopProductData(
            GEM_PRODUCT_ID, ProductType.GEM, 0, 80, ItemGrade.GENERAL, ItemGrade.GENERAL, true);

    @Mock
    private StaticDataService staticDataService;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Test
    @DisplayName("dev 도구가 꺼져 있으면 보석 상품을 거부하고 재화는 조회하지도 않는다")
    void gemProductIsRejectedWhenDevToolsDisabled() {
        when(staticDataService.getShopProducts()).thenReturn(Map.of(GEM_PRODUCT_ID, GEM_PRODUCT));
        ShopService shopService = new ShopService(staticDataService, currencyRepository, equipmentRepository, false);

        assertThatThrownBy(() -> shopService.purchase(PLAYER_ID, GEM_PRODUCT_ID))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ShopErrorCode.PAYMENT_VERIFICATION_REQUIRED));
        verifyNoInteractions(currencyRepository, equipmentRepository);
    }

    @Test
    @DisplayName("dev 도구가 켜져 있으면 로컬 테스트용으로 보석 상품을 지급한다")
    void gemProductIsGrantedWhenDevToolsEnabled() {
        when(staticDataService.getShopProducts()).thenReturn(Map.of(GEM_PRODUCT_ID, GEM_PRODUCT));
        Currency currency = new Currency(PLAYER_ID, 0, 0, 60, Instant.EPOCH);
        when(currencyRepository.findForUpdate(PLAYER_ID)).thenReturn(Optional.of(currency));
        ShopService shopService = new ShopService(staticDataService, currencyRepository, equipmentRepository, true);

        shopService.purchase(PLAYER_ID, GEM_PRODUCT_ID);

        assertThat(currency.getCurrencyGem()).isEqualTo(80);
    }
}