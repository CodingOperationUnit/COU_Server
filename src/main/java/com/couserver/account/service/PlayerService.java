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
    public PlayerSaveDataResponse loadSave(Long playerId) {
        PlayerProfile profile = playerProfileRepository.findById(playerId)   // ← findById로 변경
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "플레이어 데이터가 없습니다."));

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

    @Transactional
    public PlayerSaveDataResponse save(Long playerId, PlayerSaveRequest request) {
        PlayerProfile profile = playerProfileRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "플레이어 데이터가 없습니다."));

        // 1) 닉네임: 바꿨을 때만 중복 검사
        ProfileSaveRequest profileRequest = request.getProfile();
        String newNickname = profileRequest.getPlayerNickname();
        if (!newNickname.equals(profile.getPlayerNickname())
                && playerProfileRepository.existsByPlayerNickname(newNickname)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
        }

        // 2) 스테이지: 해금된 스테이지인지 검사
        StageProgressSaveRequest stageRequest = request.getStageProgress();
        Integer maxCleared = stageRequest.getMaxClearedStageId();
        int maxPlayable = (maxCleared == null) ? firstStageId : maxCleared + 1;
        if (stageRequest.getCurrentStageId() > maxPlayable) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해금되지 않은 스테이지입니다.");
        }

        // 3) 검사를 모두 통과한 뒤에 반영 (변경 감지로 자동 UPDATE)
        profile.updateProgress(newNickname, profileRequest.getAccountLevel(), profileRequest.getAccountExp());

        CurrencySaveRequest currencyRequest = request.getCurrency();
        getCurrency(playerId).update(
                currencyRequest.getCurrencyGold(),
                currencyRequest.getCurrencyGem(),
                currencyRequest.getCurrencyEnergy(),
                currencyRequest.getCurrencyEnergyUpdatedAt());

        getStageProgress(playerId).update(stageRequest.getCurrentStageId(), maxCleared);

        PlayerStatSaveRequest statRequest = request.getPlayerStat();
        getPlayerStat(playerId).update(
                statRequest.getPlayerStatAttackLevel(),
                statRequest.getPlayerStatHpLevel(),
                statRequest.getPlayerStatDefenseLevel());

        // TODO(18단계): inventoryList 반영

        // 4) 저장된 결과를 S1과 같은 모양으로 돌려줌
        return loadSave(playerId);
    }

    private Currency getCurrency(Long playerId) {
        return currencyRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "재화 데이터가 없습니다."));
    }

    private StageProgress getStageProgress(Long playerId) {
        return stageProgressRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "스테이지 데이터가 없습니다."));
    }

    private PlayerStat getPlayerStat(Long playerId) {
        return playerStatRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "스탯 데이터가 없습니다."));
    }

    @Transactional(readOnly = true)
    public Long getPlayerId(int accountId) {
        return playerProfileRepository.findByAccount_AccountId(accountId)
                .map(PlayerProfile::getPlayerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "플레이어 데이터가 없습니다."));
    }
}
