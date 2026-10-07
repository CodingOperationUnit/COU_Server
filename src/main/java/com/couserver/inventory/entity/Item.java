package com.couserver.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 아이템 ID
@Getter
@Entity
@Table(name = "item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item {

    @Id
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EquipSlotType slotType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemGrade grade;

    @Column(nullable = false)
    private int hpBonus;

    @Column(nullable = false)
    private int attackBonus;

    @Column(nullable = false)
    private int moveSpeedBonus;

    public Item(Long id, String name, String description,
                EquipSlotType slotType, ItemGrade grade,
                int hpBonus, int attackBonus, int moveSpeedBonus) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.slotType = slotType;
        this.grade = grade;
        this.hpBonus = hpBonus;
        this.attackBonus = attackBonus;
        this.moveSpeedBonus = moveSpeedBonus;
    }
}
