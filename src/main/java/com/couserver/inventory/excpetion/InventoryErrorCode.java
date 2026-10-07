package com.couserver.inventory.excpetion;

import com.couserver.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 인벤토리 전용 에러 코드
@Getter
@RequiredArgsConstructor
public enum InventoryErrorCode  implements ErrorCode {

    INVENTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "대상 장비를 찾을 수 없습니다."),
    NOT_OWNED_INVENTORY(HttpStatus.FORBIDDEN, "내 소유가 아닌 장비입니다."),
    NOT_EQUIPPED(HttpStatus.BAD_REQUEST, "장착 중인 장비가 아닙니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}