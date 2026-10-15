package com.couserver.player.dto;

import lombok.Getter;

// 의심 활동 기록에서 비교용으로만 읽는다
@Getter
public class PlayerStatSaveRequest {
    private Integer playerStatAttackLevel;
    private Integer playerStatHpLevel;
    private Integer playerStatDefenseLevel;
}