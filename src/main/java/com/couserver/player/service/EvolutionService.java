package com.couserver.player.service;

import com.couserver.account.dto.CurrencyResponse;
import com.couserver.account.entity.Currency;
import com.couserver.account.repository.CurrencyRepository;
import com.couserver.common.exception.BusinessException;
import com.couserver.player.dto.EvolutionUpgradeResponse;
import com.couserver.player.entity.PlayerStat;
import com.couserver.player.entity.PlayerStatType;
import com.couserver.player.exception.PlayerErrorCode;
import com.couserver.player.repository.PlayerStatRepository;
import com.couserver.ranking.event.RankingChangedEvent;
import com.couserver.staticdata.dto.EvolutionData;
import com.couserver.staticdata.service.StaticDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EvolutionService {
    private final PlayerStatRepository playerStatRepository;
    private final CurrencyRepository currencyRepository;     // 계정 담당 (잠금 조회·골드 차감에 사용)
    private final StaticDataService staticDataService;
    private final ApplicationEventPublisher eventPublisher;

    // 진화 1단계: 정해진 순서(공격력 → 체력 → 방어력 → 포션 회복량)의 다음 능력치 레벨을 1 올린다
    // TODO: 동시 요청 대비 락 — 비용이 생기면 추가 (지금은 비용 0이라 중복 처리돼도 손해 없음)
    @Transactional
    public EvolutionUpgradeResponse upgrade(Long playerId){
        // 재화 행을 먼저 잠근다. 같은 플레이어의 진화·강화·전투 요청이 차례대로 처리되어 골드가 이중으로 빠지지 않는다
        Currency currency = currencyRepository.findForUpdate(playerId)
                .orElseThrow(() -> new BusinessException(PlayerErrorCode.CURRENCY_NOT_FOUND));
        PlayerStat stat = playerStatRepository.findById(playerId)
                .orElseThrow(() -> new BusinessException(PlayerErrorCode.PLAYER_STAT_NOT_FOUND));

        EvolutionData evolution = staticDataService.getEvolution();
        int step = stat.getEvolutionStep();   // 진화 전 단계 (지금까지 진화한 횟수)

        if (evolution.isMaxStep(step)) {
            throw new BusinessException(PlayerErrorCode.EVOLUTION_MAX_STEP_REACHED);
        }

        int cost = evolution.costAt(step);
        if (currency.getCurrencyGold() < cost) {
            throw new BusinessException(PlayerErrorCode.NOT_ENOUGH_GOLD);
        }

        // 골드 차감과 레벨 상승이 한 트랜잭션: 중간에 실패하면 둘 다 롤백
        currency.spendGold(cost);
        PlayerStatType upgraded = evolution.statAt(step);
        stat.increaseLevel(upgraded);
        eventPublisher.publishEvent(new RankingChangedEvent(playerId));   // 진화 스탯이 올라 전투력이 달라진다

        return EvolutionUpgradeResponse.of(
                upgraded, evolution.statAt(step + 1), stat, cost, new CurrencyResponse(currency));
    }
}
