package com.couserver.staticdata.dto;

// skillLevelId는 이 행의 PK(예: 20011), skillId는 Skill을 가리키는 FK(예: 20010)
public record SkillLevelData(
        long skillLevelId,
        long skillId,
        int skillLevel,
        SkillEffectType effect1Type,
        double effect1Value,
        SkillEffectType effect2Type,
        double effect2Value,
        String description) {
}
