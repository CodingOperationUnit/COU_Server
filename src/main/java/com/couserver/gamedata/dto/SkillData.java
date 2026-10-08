package com.couserver.gamedata.dto;

// data/Skill.json의 datas 한 행
public record SkillData(
        int skillId,
        String skillName,
        int skillCategory,
        String skillType,
        float skillCooldown,
        float skillSpeed,
        float skillDamage,
        float skillRange,
        String skillDescription,
        String level1SkillDescription,
        String level2SkillDescription,
        String level3SkillDescription,
        String level4SkillDescription,
        String level5SkillDescription) {
}
