package com.couserver.gamedata.dto;

import java.util.List;

// GET /api/v1/data/skills 응답
public record SkillsResponse(int version, List<SkillResponse> datas) {
}
