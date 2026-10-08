package com.couserver.staticdata.dto;

public record SkillData(
        long skillId,
        String skillName,
        int skillCategory,
        SkillType skillType,
        double skillCooldown,
        double skillSpeed,
        double skillDamage,
        double skillRange,
        String skillDescription,
        String level1SkillDescription,
        String level2SkillDescription,
        String level3SkillDescription,
        String level4SkillDescription,
        String level5SkillDescription) {
}
