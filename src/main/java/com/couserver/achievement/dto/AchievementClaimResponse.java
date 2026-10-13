package com.couserver.achievement.dto;

import com.couserver.account.dto.CurrencyResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 보상 수령 결과. 클라이언트는 재화와 해당 업적 카드만 갱신하면 된다.
@Getter
@RequiredArgsConstructor
public class AchievementClaimResponse {
    private final CurrencyResponse currency;
    private final PlayerAchievementResponse achievement;
}
