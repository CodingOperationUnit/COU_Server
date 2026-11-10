package com.couserver.inventory.entity;

// 장비 등급 (낮은 순). 선언 순서 = 시트 Grade의 gradeOrder 순서 (2단계에서 시작 시 검사)
public enum ItemGrade {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY;

    // 같은 아이템·같은 등급 2개로 다음 등급을 만들 수 있는지
    // 임시: Epic → Legendary 합성 규칙(미확정 5)이 정해질 때까지 Epic 이상은 합성할 수 없다
    public boolean canSynthesize() {
        return ordinal() < LEGENDARY.ordinal();
    }

    public ItemGrade next() {
        if (!canSynthesize()) {
            throw new IllegalStateException(
                    "Legendary 등급은 더 이상 합성할 수 없습니다.");
        }

        return values()[ordinal() + 1];
    }
    public String toClientName() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
