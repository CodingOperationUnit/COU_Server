package com.couserver.monster.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "monsters")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Monster {

    @Id
    private Integer monsterId;

    @Column(nullable = false)
    private String monsterName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MonsterType monsterType;

    private int monsterMaxHealthPoint;
    private int monsterAttackPoint;
    private int monsterDefensePoint;
    private float monsterMoveSpeed;
    private int monsterExp;
    private String monsterAsset;
}
