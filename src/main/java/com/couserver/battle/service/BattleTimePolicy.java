package com.couserver.battle.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 전투 시간 검증 설정. 서버가 잰 경과 시간으로부터, 클라이언트 전투에서 흐를 수 있는 최대 게임 시간을 구한다.
// - 정식 클라이언트는 배속이 없어 게임 시간이 실제 시간을 넘지 않는다 (기본 배율 1.0)
// - 배속 기능이 생기면 battle.max-speed-multiplier를 최대 배율로 올린다
// - dev 도구가 켜져 있으면 DevPanel 배속 등으로 테스트할 수 있게 시간 검증을 건너뛴다
@Slf4j
@Component
public class BattleTimePolicy {
    private final boolean devToolsEnabled;
    private double maxSpeedMultiplier;   // 테스트에서 바꿔 끼울 수 있게 final로 두지 않는다

    public BattleTimePolicy(@Value("${dev-tools.enabled:false}") boolean devToolsEnabled,
                            @Value("${battle.max-speed-multiplier:1.0}") double maxSpeedMultiplier) {
        if (maxSpeedMultiplier <= 0) {
            throw new IllegalStateException("battle.max-speed-multiplier는 0보다 커야 합니다: " + maxSpeedMultiplier);
        }
        this.devToolsEnabled = devToolsEnabled;
        this.maxSpeedMultiplier = maxSpeedMultiplier;
        if (devToolsEnabled) {
            log.warn("dev 도구가 켜져 있어 전투 시간 검증(최소 승리 시간, 시간 비례 상한)을 건너뜁니다. 운영 환경에서는 DEV_TOOLS_ENABLED를 꺼야 합니다.");
        }
    }

    // 입력은 서버 기준 경과 시간(초)에 시계 차이 허용 시간을 더한 값. 시간 검증을 건너뛰면 무한대
    public double maxGameSeconds(long elapsedSecondsWithTolerance) {
        if (devToolsEnabled) {
            return Double.POSITIVE_INFINITY;
        }
        return elapsedSecondsWithTolerance * maxSpeedMultiplier;
    }
}