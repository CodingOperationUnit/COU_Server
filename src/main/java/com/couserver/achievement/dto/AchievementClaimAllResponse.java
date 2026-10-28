package com.couserver.achievement.dto;

import com.couserver.account.dto.CurrencyResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

// 업적 보상 모두 받기 결과. 클라이언트는 재화를 currency로 덮어쓰고, achievements에 있는 업적 카드만 갱신하면 된다.
@Getter
@RequiredArgsConstructor
public class AchievementClaimAllResponse {
    private final CurrencyResponse currency;                    // 지급 후 재화 전체
    private final List<PlayerAchievementResponse> achievements; // 이번에 수령한 업적만, 업적 ID 오름차순
    private final long totalGold;                               // 지급한 골드 보상 합계 (보상표 기준)
    private final long totalGem;                                // 지급한 젬 보상 합계 (보상표 기준)
    private final int claimedStepCount;                         // 이번에 수령한 단계 수 (모든 업적 합)
}