package com.couserver.account.service;

import com.couserver.account.dto.*;
import com.couserver.account.entity.*;
import com.couserver.account.repository.CurrencyRepository;
import com.couserver.account.repository.PlayerProfileRepository;
import com.couserver.account.repository.PlayerStatRepository;
import com.couserver.account.repository.StageProgressRepository;
import com.couserver.inventory.entity.Equipment;
import com.couserver.inventory.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerService {
    private final PlayerProfileRepository playerProfileRepository;
    private final CurrencyRepository currencyRepository;
    private final StageProgressRepository stageProgressRepository;
    private final PlayerStatRepository playerStatRepository;
    private final EquipmentRepository equipmentRepository;

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

    @Transactional(readOnly = true)
    public PlayerSaveDataResponse loadSave(int accountId) {
        PlayerProfile profile = playerProfileRepository.findByAccount_AccountId(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "플레이어 데이터가 없습니다."));
        Long playerId = profile.getPlayerId();

        Currency currency = currencyRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "재화 데이터가 없습니다."));
        StageProgress stageProgress = stageProgressRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "스테이지 데이터가 없습니다."));
        PlayerStat playerStat = playerStatRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "스탯 데이터가 없습니다."));

        List<Equipment> equipments = equipmentRepository.findByPlayerIdOrderByIdAsc(playerId);   // ← 한 번만 조회

        List<InventoryItemResponse> inventoryList = equipments.stream()
                .map(InventoryItemResponse::new)
                .toList();

        return new PlayerSaveDataResponse(
                new PlayerProfileResponse(profile, equipments),
                new CurrencyResponse(currency),
                inventoryList,
                new StageProgressResponse(stageProgress),
                new PlayerStatResponse(playerStat)
        );
    }

    @Transactional(readOnly = true)
    public Long getPlayerId(int accountId) {
        return playerProfileRepository.findByAccount_AccountId(accountId)
                .map(PlayerProfile::getPlayerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "플레이어 데이터가 없습니다."));
    }
}
