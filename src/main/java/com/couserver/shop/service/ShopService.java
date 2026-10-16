package com.couserver.shop.service;

import com.couserver.account.entity.Currency;
import com.couserver.account.repository.CurrencyRepository;
import com.couserver.common.exception.BusinessException;
import com.couserver.inventory.dto.EquipmentResponse;
import com.couserver.inventory.entity.Equipment;
import com.couserver.inventory.repository.EquipmentRepository;
import com.couserver.shop.dto.ProductResponse;
import com.couserver.shop.dto.PurchaseResponse;
import com.couserver.shop.exception.ShopErrorCode;
import com.couserver.staticdata.dto.ItemData;
import com.couserver.staticdata.dto.ProductType;
import com.couserver.staticdata.dto.ShopProductData;
import com.couserver.staticdata.service.StaticDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class ShopService {

    private final StaticDataService staticDataService;
    private final CurrencyRepository currencyRepository;
    private final EquipmentRepository equipmentRepository;
    private final boolean devToolsEnabled;

    public ShopService(StaticDataService staticDataService,
                       CurrencyRepository currencyRepository,
                       EquipmentRepository equipmentRepository,
                       @Value("${dev-tools.enabled:false}") boolean devToolsEnabled) {
        this.staticDataService = staticDataService;
        this.currencyRepository = currencyRepository;
        this.equipmentRepository = equipmentRepository;
        this.devToolsEnabled = devToolsEnabled;
        if (devToolsEnabled) {
            log.warn("dev 도구가 켜져 있어 보석 상품을 결제 없이 지급합니다. 운영 환경에서는 DEV_TOOLS_ENABLED를 꺼야 합니다.");
        }
    }

    // 판매 중인 상품 목록
    public List<ProductResponse> getProducts() {
        return staticDataService.getShopProducts().values().stream()
                .filter(ShopProductData::active)
                .map(this::toProductResponse)
                .toList();
    }

    @Transactional
    public PurchaseResponse purchase(Long playerId, int productId) {
        // 1. 검증은 전부 젬을 쓰기 전에 끝낸다
        ShopProductData product = staticDataService.getShopProducts().get(productId);
        if (product == null) {
            throw new BusinessException(ShopErrorCode.PRODUCT_NOT_FOUND);
        }
        if (!product.active()) {
            throw new BusinessException(ShopErrorCode.PRODUCT_NOT_AVAILABLE);
        }
        // 보석 상품은 실제 결제 상품이라 영수증 검증 없이는 지급하지 않는다 (F2).
        // 로컬 테스트용으로 dev 도구가 켜져 있을 때만 허용한다
        if (product.productType() == ProductType.GEM && !devToolsEnabled) {
            throw new BusinessException(ShopErrorCode.PAYMENT_VERIFICATION_REQUIRED);
        }

        Currency currency = currencyRepository.findForUpdate(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "재화 데이터가 없습니다."));
        if (currency.getCurrencyGem() < product.priceGem()) {
            throw new BusinessException(ShopErrorCode.NOT_ENOUGH_CURRENCY);
        }


        currency.spendGem(product.priceGem());

        List<EquipmentResponse> rewardedItems = new ArrayList<>();
        switch (product.productType()) {
            case GOLD -> currency.addGold(product.rewardAmount());
            case GEM -> currency.addGem(product.rewardAmount());
            case RANDOMITEM -> rewardedItems.add(drawEquipment(playerId, product));
        }

        return new PurchaseResponse(
                product.productId(), product.priceGem(),
                currency.getCurrencyGold(), currency.getCurrencyGem(), rewardedItems);
    }

    // 등급 범위 안의 장비 중 하나를 뽑아 지급한다
    private EquipmentResponse drawEquipment(Long playerId, ShopProductData product) {
        List<ItemData> candidates = staticDataService.getItems().values().stream()
                .filter(i -> i.grade().ordinal() >= product.minGrade().ordinal()
                        && i.grade().ordinal() <= product.maxGrade().ordinal())
                .toList();
        ItemData picked = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));

        Equipment saved = equipmentRepository.save(Equipment.create(playerId, picked));
        return new EquipmentResponse(
                saved.getId(), saved.getItemId(), saved.getLevel(),
                saved.getGrade().toClientName(), saved.isEquipped());
    }

    private ProductResponse toProductResponse(ShopProductData p) {
        boolean box = p.productType() == ProductType.RANDOMITEM;
        return new ProductResponse(
                p.productId(), toClientType(p.productType()), p.priceGem(), p.rewardAmount(),
                box ? p.minGrade().toClientName() : null,
                box ? p.maxGrade().toClientName() : null);
    }

    private String toClientType(ProductType type) {
        return switch (type) {
            case GOLD -> "Gold";
            case GEM -> "Gem";
            case RANDOMITEM -> "RandomItem";
        };
    }
}