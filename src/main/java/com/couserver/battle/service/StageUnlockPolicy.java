package com.couserver.battle.service;

import com.couserver.staticdata.service.StaticDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

// 스테이지 해금 규칙. 전투 입장과 save의 스테이지 선택이 함께 쓴다
@Component
@RequiredArgsConstructor
public class StageUnlockPolicy {
    private final StaticDataService staticDataService;

    public boolean isUnlocked(int stageId, Integer maxClearedStageId) {
        return isUnlocked(staticDataService.getStages().keySet(), stageId, maxClearedStageId);
    }

    // 정적 데이터에 있는 스테이지이고, 그보다 앞의 스테이지를 모두 클리어했으면 해금된 것이다.
    // 스테이지 ID가 연속이 아니어도 된다 (예: 1, 2, 5에서 2를 클리어하면 5가 열린다)
    public static boolean isUnlocked(Set<Integer> stageIds, int stageId, Integer maxClearedStageId) {
        if (!stageIds.contains(stageId)) {
            return false;
        }
        return stageIds.stream()
                .noneMatch(id -> id < stageId && (maxClearedStageId == null || id > maxClearedStageId));
    }
}