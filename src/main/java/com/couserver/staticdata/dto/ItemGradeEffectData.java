package com.couserver.staticdata.dto;

import com.couserver.inventory.entity.ItemGrade;

// ItemGradeEffect 시트 (아이템·등급별 효과 연결). 같은 (itemId, grade)에 행이 여러 개면 그 등급의 효과가 여러 개다
public record ItemGradeEffectData(
        int itemGradeEffectId,
        long itemId,        // Item의 itemId (FK)
        ItemGrade grade,    // 이 등급에서 해금
        long effectId) {    // EquipEffect의 effectId (FK)
}