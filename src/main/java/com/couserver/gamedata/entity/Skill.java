package com.couserver.gamedata.entity;

import com.couserver.gamedata.dto.SkillData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 스킬 기획 데이터 한 행. 원본은 data/Skill.json이고 서버 시작 시 통째로 교체된다.
@Getter
@Entity
@Table(name = "skill")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Skill {

    @Id
    private Integer skillId;

    @Column(nullable = false, length = 100)
    private String skillName;

    // 0 액티브, 1 패시브
    @Column(nullable = false)
    private int skillCategory;

    // Nearest, Forward, None (클라이언트가 문자열 그대로 비교하므로 enum으로 바꾸지 않는다)
    @Column(nullable = false, length = 20)
    private String skillType;

    // MySQL FLOAT는 유효숫자 6자리로 읽혀 값이 깎이므로(1.2345678 -> 1.23457) DOUBLE 열에 저장한다.
    // 필드는 float라서 응답 타입은 그대로다.
    @Column(nullable = false, columnDefinition = "double")
    private float skillCooldown;

    @Column(nullable = false, columnDefinition = "double")
    private float skillSpeed;

    @Column(nullable = false, columnDefinition = "double")
    private float skillDamage;

    @Column(nullable = false, columnDefinition = "double")
    private float skillRange;

    @Column(nullable = false, length = 500)
    private String skillDescription;

    @Column(name = "level1_skill_description", nullable = false, length = 500)
    private String level1SkillDescription;

    @Column(name = "level2_skill_description", nullable = false, length = 500)
    private String level2SkillDescription;

    @Column(name = "level3_skill_description", nullable = false, length = 500)
    private String level3SkillDescription;

    @Column(name = "level4_skill_description", nullable = false, length = 500)
    private String level4SkillDescription;

    @Column(name = "level5_skill_description", nullable = false, length = 500)
    private String level5SkillDescription;

    public static Skill from(SkillData d) {
        Skill skill = new Skill();
        skill.skillId = d.skillId();
        skill.skillName = d.skillName();
        skill.skillCategory = d.skillCategory();
        skill.skillType = d.skillType();
        skill.skillCooldown = d.skillCooldown();
        skill.skillSpeed = d.skillSpeed();
        skill.skillDamage = d.skillDamage();
        skill.skillRange = d.skillRange();
        skill.skillDescription = d.skillDescription();
        skill.level1SkillDescription = d.level1SkillDescription();
        skill.level2SkillDescription = d.level2SkillDescription();
        skill.level3SkillDescription = d.level3SkillDescription();
        skill.level4SkillDescription = d.level4SkillDescription();
        skill.level5SkillDescription = d.level5SkillDescription();
        return skill;
    }
}
