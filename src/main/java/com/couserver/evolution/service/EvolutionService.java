package com.couserver.evolution.service;

import com.couserver.account.dto.CurrencyResponse;
import com.couserver.account.entity.Currency;
import com.couserver.account.entity.PlayerProfile;
import com.couserver.account.repository.CurrencyRepository;
import com.couserver.account.repository.PlayerProfileRepository;
import com.couserver.common.exception.BusinessException;
import com.couserver.evolution.dto.EvolutionUpgradeResponse;
import com.couserver.evolution.exception.EvolutionErrorCode;
import com.couserver.player.entity.PlayerStat;
import com.couserver.player.entity.PlayerStatType;
import com.couserver.player.exception.PlayerErrorCode;
import com.couserver.player.repository.PlayerStatRepository;
import com.couserver.ranking.event.RankingChangedEvent;
import com.couserver.staticdata.dto.EvolutionData;
import com.couserver.staticdata.service.StaticDataService;
import com.couserver.ledger.entity.LedgerReason;
import com.couserver.ledger.service.CurrencyLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EvolutionService {
    private final PlayerStatRepository playerStatRepository;
    private final CurrencyRepository currencyRepository;     // 계정 담당 (잠금 조회·골드 차감에 사용)
    private final PlayerProfileRepository playerProfileRepository;
    private final StaticDataService staticDataService;
    private final CurrencyLedgerService currencyLedger;
    private final ApplicationEventPublisher eventPublisher;

    // 진화 1단계: 시트의 진화 순서에 따라 다음 스탯의 레벨을 1 올리고, 그 비용만큼 골드를 차감한다
    // 검사 순서: 최대 단계 → 계정 레벨 제한 → 골드 (최대 단계에 도달했으면 레벨을 올려도 소용없으므로 먼저 알린다)
    @Transactional
    public EvolutionUpgradeResponse upgrade(Long playerId){
        // 재화 행을 먼저 잠근다. 같은 플레이어의 진화·강화·전투 요청이 차례대로 처리되어 골드가 이중으로 빠지지 않는다
        Currency currency = currencyRepository.findForUpdate(playerId)
                .orElseThrow(() -> new BusinessException(EvolutionErrorCode.CURRENCY_NOT_FOUND));
        PlayerStat stat = playerStatRepository.findById(playerId)
                .orElseThrow(() -> new BusinessException(PlayerErrorCode.PLAYER_STAT_NOT_FOUND));
        PlayerProfile profile = playerProfileRepository.findById(playerId)
                .orElseThrow(() -> new BusinessException(EvolutionErrorCode.PLAYER_PROFILE_NOT_FOUND));

        EvolutionData evolution = staticDataService.getEvolution();
        int step = stat.getEvolutionStep();   // 진화 전 단계 (지금까지 진화한 횟수)

        if (evolution.isMaxStep(step)) {
            throw new BusinessException(EvolutionErrorCode.EVOLUTION_MAX_STEP_REACHED);
        }
        if (evolution.isLevelLocked(step, profile.getAccountLevel())) {
            throw new BusinessException(EvolutionErrorCode.EVOLUTION_LEVEL_LOCKED);
        }

        int cost = evolution.costAt(step);
        if (currency.getCurrencyGold() < cost) {
            throw new BusinessException(EvolutionErrorCode.NOT_ENOUGH_GOLD);
        }

        // 골드 차감과 레벨 상승이 한 트랜잭션: 중간에 실패하면 둘 다 롤백
        currencyLedger.spendGold(currency, cost, LedgerReason.EVOLUTION, (long) (step + 1));   // 진화 후 단계
        PlayerStatType upgraded = evolution.statAt(step);
        stat.increaseLevel(upgraded);
        eventPublisher.publishEvent(new RankingChangedEvent(playerId));   // 진화 스탯이 올라 전투력이 달라진다

        return EvolutionUpgradeResponse.of(
                upgraded, evolution.statAt(step + 1), stat, cost, new CurrencyResponse(currency));
    }
}
