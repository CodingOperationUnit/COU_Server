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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final StaticDataService staticDataService;
    private final CurrencyRepository currencyRepository;
    private final EquipmentRepository equipmentRepository;

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