package com.couserver.account.service;

import com.couserver.account.dto.*;
import com.couserver.account.entity.*;
import com.couserver.account.repository.CurrencyRepository;
import com.couserver.account.repository.PlayerProfileRepository;
import com.couserver.player.dto.PlayerStatResponse;
import com.couserver.player.dto.PlayerStatSaveRequest;
import com.couserver.player.repository.PlayerStatRepository;
import com.couserver.account.repository.StageProgressRepository;
import com.couserver.battle.dto.StageRecordResponse;
import com.couserver.battle.repository.StageRecordRepository;
import com.couserver.player.entity.PlayerStat;
import com.couserver.staticdata.dto.AccountConstData;
import com.couserver.staticdata.service.StaticDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerService {
    private final PlayerProfileRepository playerProfileRepository;
    private final CurrencyRepository currencyRepository;
    private final StageProgressRepository stageProgressRepository;
    private final PlayerStatRepository playerStatRepository;
    private final StageRecordRepository stageRecordRepository;
    private final StaticDataService staticDataService;
    private final Clock clock;

    @Transactional
    public void createInitialData(Account account,String playerNickname) {
        PlayerProfile profile = playerProfileRepository.save(new PlayerProfile(account, playerNickname));
        Long playerId = profile.getPlayerId();

        AccountConstData accountConst = staticDataService.getAccountConst();
        currencyRepository.save(new Currency(playerId, accountConst.initialGold(), accountConst.initialGem(),
                accountConst.initialStamina(), Instant.now(clock)));
        stageProgressRepository.save(new StageProgress(playerId, staticDataService.getFirstStageId()));
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
        List<StageRecordResponse> stageRecords = stageRecordRepository.findByPlayerIdOrderByStageIdAsc(playerId)
                .stream()
                .map(StageRecordResponse::new)
                .toList();

        // 회복한 스태미나를 응답에 반영한다. 읽기 전용이라 저장하지 않지만, 같은 시각과 공식이라 언제 계산해도 결과가 같다
        AccountConstData accountConst = staticDataService.getAccountConst();
        currency.recoverEnergy(Instant.now(clock), accountConst.maxStamina(), accountConst.staminaRecoverySeconds());

        return new PlayerSaveDataResponse(
                new PlayerProfileResponse(profile),
                new CurrencyResponse(currency),
                new StageProgressResponse(stageProgress),
                new PlayerStatResponse(playerStat),
                stageRecords
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
        int maxPlayable = (maxCleared == null) ? staticDataService.getFirstStageId() : maxCleared + 1;
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
