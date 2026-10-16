package com.couserver.shop.exception;

import com.couserver.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 상점 전용 에러 코드
@Getter
@RequiredArgsConstructor
public enum ShopErrorCode implements ErrorCode {

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 상품입니다."),
    PRODUCT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "판매 중이 아닌 상품입니다."),
    NOT_ENOUGH_CURRENCY(HttpStatus.BAD_REQUEST, "재화가 부족합니다."),
    PAYMENT_VERIFICATION_REQUIRED(HttpStatus.FORBIDDEN, "보석 상품은 결제 기능 준비 중이라 구매할 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
