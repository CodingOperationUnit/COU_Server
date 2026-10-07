package com.couserver.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

//플레이어가 보유한 장비
@Getter
@Entity
@Table(name = "equipment", indexes = @Index(name = "idx_equipment_player", columnList = "playerId"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Equipment {

    //장비 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 장비를 가지고있는 플레이어 아이디
    @Column(nullable = false)
    private Long playerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private int level = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemGrade grade;

    @Column(nullable = false)
    private boolean equipped = false;

    // 장비 획득 시각
    @Column(nullable = false, updatable = false)
    private LocalDateTime acquiredAt;

    public static Equipment create(Long playerId, Item item) {
        Equipment equipment = new Equipment();
        equipment.playerId = playerId;
        equipment.item = item;
        equipment.grade = item.getGrade();
        equipment.acquiredAt = LocalDateTime.now();
        return equipment;
    }

    public void equip() {
        this.equipped = true;
    }

    public void unequip() {
        this.equipped = false;
    }

}
