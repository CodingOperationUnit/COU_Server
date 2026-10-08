package com.couserver.gamedata.dto;

import com.couserver.gamedata.entity.Skill;

// GET /api/v1/data/skills 의 datas 항목. 필드 이름과 타입은 명세와 클라이언트 파서에 맞춘다.
public record SkillResponse(
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

    public static SkillResponse from(Skill s) {
        return new SkillResponse(
                s.getSkillId(),
                s.getSkillName(),
                s.getSkillCategory(),
                s.getSkillType(),
                s.getSkillCooldown(),
                s.getSkillSpeed(),
                s.getSkillDamage(),
                s.getSkillRange(),
                s.getSkillDescription(),
                s.getLevel1SkillDescription(),
                s.getLevel2SkillDescription(),
                s.getLevel3SkillDescription(),
                s.getLevel4SkillDescription(),
                s.getLevel5SkillDescription());
    }
}
