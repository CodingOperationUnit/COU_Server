package com.couserver.inventory.entity;

// 장비 등급
public enum ItemGrade {
    GENERAL,
    SUPER,
    RARE;

    public boolean canSynthesize() {
        return this != RARE;
    }

    public ItemGrade next() {
        return values()[ordinal() + 1];
    }
    public String toClientName() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
