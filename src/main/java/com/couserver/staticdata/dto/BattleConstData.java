package com.couserver.staticdata.dto;

// BattleConst 중 서버가 읽는 열만 둔다
public record BattleConstData(
        double boxSpawnInterval,
        int qualityMax) {
}
