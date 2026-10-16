package com.couserver.shop;

import com.couserver.account.entity.Currency;
import com.couserver.account.repository.CurrencyRepository;
import com.couserver.common.exception.BusinessException;
import com.couserver.inventory.entity.ItemGrade;
import com.couserver.inventory.repository.EquipmentRepository;
import com.couserver.ledger.entity.CurrencyLedger;
import com.couserver.ledger.entity.LedgerReason;
import com.couserver.ledger.repository.CurrencyLedgerRepository;
import com.couserver.ledger.service.CurrencyLedgerService;
import com.couserver.shop.exception.ShopErrorCode;
import com.couserver.shop.service.ShopService;
import com.couserver.staticdata.dto.ProductType;
import com.couserver.staticdata.dto.ShopProductData;
import com.couserver.staticdata.service.StaticDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
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

    @Mock
    private CurrencyLedgerRepository ledgerRepository;

    @Test
    @DisplayName("dev 도구가 꺼져 있으면 보석 상품을 거부하고 재화는 조회하지도 않는다")
    void gemProductIsRejectedWhenDevToolsDisabled() {
        when(staticDataService.getShopProducts()).thenReturn(Map.of(GEM_PRODUCT_ID, GEM_PRODUCT));
        ShopService shopService = shopService(false);

        assertThatThrownBy(() -> shopService.purchase(PLAYER_ID, GEM_PRODUCT_ID))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ShopErrorCode.PAYMENT_VERIFICATION_REQUIRED));
        verifyNoInteractions(currencyRepository, equipmentRepository, ledgerRepository);
    }

    @Test
    @DisplayName("dev 도구가 켜져 있으면 로컬 테스트용으로 보석 상품을 지급하고 원장에 남긴다")
    void gemProductIsGrantedWhenDevToolsEnabled() {
        when(staticDataService.getShopProducts()).thenReturn(Map.of(GEM_PRODUCT_ID, GEM_PRODUCT));
        Currency currency = new Currency(PLAYER_ID, 0, 80, 60, Instant.EPOCH);
        when(currencyRepository.findForUpdate(PLAYER_ID)).thenReturn(Optional.of(currency));
        ShopService shopService = shopService(true);

        shopService.purchase(PLAYER_ID, GEM_PRODUCT_ID);

        assertThat(currency.getCurrencyGem()).isIn(0, 160);
        ArgumentCaptor<CurrencyLedger> saved = ArgumentCaptor.forClass(CurrencyLedger.class);
        verify(ledgerRepository).save(saved.capture());   // 가격 0의 차감은 기록하지 않고, 지급 한 줄만 남는다
        assertThat(saved.getValue().getDelta()).isIn(80L, -80L);
        assertThat(saved.getValue().getReason()).isEqualTo(LedgerReason.SHOP_PURCHASE);
    }

    // 단위 테스트에는 프록시가 없어 원장 서비스의 트랜잭션 검사 없이 동작한다. 저장소만 가짜로 둔다
    private ShopService shopService(boolean devToolsEnabled) {
        CurrencyLedgerService ledger = new CurrencyLedgerService(ledgerRepository, Clock.systemUTC());
        return new ShopService(staticDataService, currencyRepository, equipmentRepository, ledger, devToolsEnabled);
    }
}