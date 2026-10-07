package com.couserver.player.service;

import com.couserver.common.exception.BusinessException;
import com.couserver.player.dto.EvolutionUpgradeResponse;
import com.couserver.player.dummy.EvolutionDummyData;
import com.couserver.player.entity.PlayerStat;
import com.couserver.player.entity.PlayerStatType;
import com.couserver.player.exception.PlayerErrorCode;
import com.couserver.player.repository.PlayerStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EvolutionService {
    private final PlayerStatRepository playerStatRepository;

    // 진화 1단계: 정해진 순서(공격력 → 체력 → 방어력 → 포션 회복량)의 다음 능력치 레벨을 1 올린다
    // TODO: 동시 요청 대비 락 — 비용이 생기면 추가 (지금은 비용 0이라 중복 처리돼도 손해 없음)
    @Transactional
    public EvolutionUpgradeResponse upgrade(Long playerId){
        PlayerStat stat = playerStatRepository.findById(playerId)
                .orElseThrow(() -> new BusinessException(PlayerErrorCode.PLAYER_STAT_NOT_FOUND));

        int goldCost = EvolutionDummyData.getGoldCost(stat.getEvolutionStep());
        // TODO : goldCost -> 0 이 되면 골드 확인/차감 (계정 담당 currency에 차감) 추후 협의 필요

        PlayerStatType upgraded = stat.upgradeNext();

        return EvolutionUpgradeResponse.of(upgraded, stat, goldCost);
    }
}
