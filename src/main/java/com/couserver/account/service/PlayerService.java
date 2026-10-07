package com.couserver.account.service;

import com.couserver.account.entity.*;
import com.couserver.account.repository.CurrencyRepository;
import com.couserver.account.repository.PlayerProfileRepository;
import com.couserver.account.repository.PlayerStatRepository;
import com.couserver.account.repository.StageProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerService {
    private final PlayerProfileRepository playerProfileRepository;
    private final CurrencyRepository currencyRepository;
    private final StageProgressRepository stageProgressRepository;
    private final PlayerStatRepository playerStatRepository;

    @Value("${game.initial.gold}")
    private int initialGold;

    @Value("${game.initial.gem}")
    private int initialGem;

    @Value("${game.initial.energy}")
    private int initialEnergy;

    @Value("${game.first-stage-id}")
    private int firstStageId;

    @Transactional
    public void createInitialData(Account account,String playerNickname) {
        PlayerProfile profile = playerProfileRepository.save(new PlayerProfile(account, playerNickname));
        Long playerId = profile.getPlayerId();

        currencyRepository.save(new Currency(playerId, initialGold, initialGem, initialEnergy));
        stageProgressRepository.save(new StageProgress(playerId, firstStageId));
        playerStatRepository.save(new PlayerStat(playerId));
    }
}
