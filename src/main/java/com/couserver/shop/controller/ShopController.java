package com.couserver.shop.controller;

import com.couserver.account.dto.AuthAccount;
import com.couserver.shop.dto.ProductResponse;
import com.couserver.shop.dto.PurchaseRequest;
import com.couserver.shop.dto.PurchaseResponse;
import com.couserver.shop.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    // 판매 중인 상품 목록
    @GetMapping("/products")
    public List<ProductResponse> getProducts() {
        return shopService.getProducts();
    }

    // 상품 구매
    @PostMapping("/purchase")
    public PurchaseResponse purchase(@AuthenticationPrincipal AuthAccount authAccount,
                                     @Valid @RequestBody PurchaseRequest request) {
        return shopService.purchase(authAccount.getPlayerId(), request.productId());
    }
}