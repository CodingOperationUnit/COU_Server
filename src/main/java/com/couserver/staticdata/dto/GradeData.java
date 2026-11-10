package com.couserver.staticdata.dto;

import com.couserver.inventory.entity.ItemGrade;

import java.math.BigDecimal;

public record GradeData(
        int gradeOrder,               // 1부터 (Common = 1)
        ItemGrade grade,              // 시트 "Common" → COMMON
        BigDecimal statMultiplier,    // 장비 스탯 배율 (3단계에서 ItemConst 대신 사용). 정확한 십진수로 읽는다
        boolean canDrop) {            // false면 일반 획득(드롭·상자) 불가. Legendary = 합성 전용
}