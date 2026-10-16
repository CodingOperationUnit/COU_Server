package com.couserver.staticdata.dto;

public record SkillData(
        long skillId,
        String skillName,
        int skillCategory,
        SkillTargetType skillTargetType,
        double skillCooldown,
        double skillSpeed,
        double skillDamage,
        double skillRange,
        String skillDescription) {
}
