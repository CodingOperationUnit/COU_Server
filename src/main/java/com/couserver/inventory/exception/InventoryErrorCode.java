package com.couserver.inventory.exception;

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
    NOT_EQUIPPED(HttpStatus.BAD_REQUEST, "장착 중인 장비가 아닙니다."),
    NOT_ENOUGH_MATERIAL(HttpStatus.BAD_REQUEST, "합성 재료가 부족합니다. (장착 중인 장비는 재료로 사용할 수 없습니다)"),
    INVALID_MATERIAL_SELECTION(HttpStatus.BAD_REQUEST, "합성 재료는 서로 다른 장비 2개를 선택해야 합니다."),
    MATERIAL_IS_TARGET(HttpStatus.BAD_REQUEST, "합성 대상 장비는 재료로 사용할 수 없습니다."),
    MATERIAL_MISMATCH(HttpStatus.BAD_REQUEST, "합성 재료는 대상과 같은 장비, 같은 등급이어야 합니다."),
    MATERIAL_EQUIPPED(HttpStatus.BAD_REQUEST, "장착 중인 장비는 합성 재료로 사용할 수 없습니다."),
    CANNOT_SYNTHESIZE_MAX_GRADE(HttpStatus.BAD_REQUEST, "이미 최고 등급이라 합성할 수 없습니다."),
    MAX_LEVEL_REACHED(HttpStatus.BAD_REQUEST, "이미 최대 레벨입니다."),
    NOT_ENOUGH_GOLD(HttpStatus.BAD_REQUEST, "골드가 부족합니다.");
    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}