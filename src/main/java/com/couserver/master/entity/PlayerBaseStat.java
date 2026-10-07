package com.couserver.master.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "player_base_stat")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerBaseStat {
    public static final Long SINGLE_ID = 1l;

    @Id
    private Long id;

    @Column(nullable = false)
    private int playerBaseAttack;

    @Column(nullable = false)
    private int playerBaseHp;

    // 치명타 피해
    @Column(nullable = false)
    private int playerBaseCriticalDamage;

    // 치명타 확률
    @Column(nullable = false)
    private int playerBaseCriticalChance;

    @Column(nullable = false)
    private int playerBaseSkillDamage;

    @Column(nullable = false)
    private float playerBaseMoveSpeed;

    @Column(nullable = false)
    private float playerBaseMaxMoveSpeed;

    @Column(nullable = false)
    private float playerBaseLootRadius;

    public static PlayerBaseStat create(int attack, int hp, int criticalDamage, int criticalChance, int skillDamage,
                                        float moveSpeed, float maxMoveSpeed, float lootRadius){
        PlayerBaseStat stat = new PlayerBaseStat();
        stat.id = SINGLE_ID;
        stat.update(attack, hp, criticalDamage, criticalChance, skillDamage, moveSpeed, maxMoveSpeed, lootRadius);

        return stat;
    }

    public void update(int attack, int hp, int criticalDamage, int criticalChance, int skillDamage,
                       float moveSpeed, float maxMoveSpeed, float lootRadius){
        this.playerBaseAttack = attack;
        this.playerBaseHp = hp;
        this.playerBaseCriticalDamage = criticalDamage;
        this.playerBaseCriticalChance = criticalChance;
        this.playerBaseSkillDamage = skillDamage;
        this.playerBaseMoveSpeed = moveSpeed;
        this.playerBaseMaxMoveSpeed = maxMoveSpeed;
        this.playerBaseLootRadius = lootRadius;
    }
}
