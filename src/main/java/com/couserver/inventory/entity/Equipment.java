package com.couserver.inventory.entity;

import com.couserver.staticdata.dto.ItemData;
import jakarta.persistence.*;
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

    @Column(name = "item_id", nullable = false)
    private Long itemId;

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

    public static Equipment create(Long playerId, ItemData item) {
        Equipment equipment = new Equipment();
        equipment.playerId = playerId;
        equipment.itemId = item.itemId();
        equipment.grade = item.grade();
        equipment.acquiredAt = LocalDateTime.now();
        return equipment;
    }

    public void equip() {
        this.equipped = true;
    }

    public void unequip() {
        this.equipped = false;
    }

    public void upgradeGrade() {
        this.grade = this.grade.next();
    }
    public void levelUp() {this.level++;}

}
