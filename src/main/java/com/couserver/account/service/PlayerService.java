package com.couserver.account.service;

import com.couserver.account.dto.*;
import com.couserver.account.entity.*;
import com.couserver.account.repository.CurrencyRepository;
import com.couserver.account.repository.PlayerProfileRepository;
import com.couserver.account.repository.StageProgressRepository;
import com.couserver.audit.entity.SuspiciousActivityType;
import com.couserver.audit.service.SuspiciousActivityRecorder;
import com.couserver.battle.dto.StageRecordResponse;
import com.couserver.battle.repository.StageRecordRepository;
import com.couserver.battle.service.StageUnlockPolicy;
import com.couserver.chapterchest.entity.ChapterChestClaim;
import com.couserver.chapterchest.repository.ChapterChestClaimRepository;
import com.couserver.inventory.entity.Equipment;
import com.couserver.inventory.entity.ItemGrade;
import com.couserver.inventory.repository.EquipmentRepository;
import com.couserver.player.dto.PlayerStatResponse;
import com.couserver.player.dto.PlayerStatSaveRequest;
import com.couserver.player.entity.PlayerStat;
import com.couserver.player.repository.PlayerStatRepository;
import com.couserver.ranking.event.RankingChangedEvent;
import com.couserver.staticdata.dto.AccountConstData;
import com.couserver.staticdata.dto.ItemData;
import com.couserver.staticdata.service.StaticDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerService {
    // 신규 계정에 지급·장착하는 시작 장비: 펄스 키보드(Weapon / General). 클라이언트 시작 공격 스킬이 장착 무기에서 정해진다
    private static final long STARTER_WEAPON_ITEM_ID = 6001L;

    private final PlayerProfileRepository playerProfileRepository;
    private final CurrencyRepository currencyRepository;
    private final StageProgressRepository stageProgressRepository;
    private final PlayerStatRepository playerStatRepository;
    private final StageRecordRepository stageRecordRepository;
    private final ChapterChestClaimRepository chapterChestClaimRepository;
    private final EquipmentRepository equipmentRepository;
    private final StaticDataService staticDataService;
    private final StageUnlockPolicy stageUnlockPolicy;
    private final SuspiciousActivityRecorder suspiciousActivityRecorder;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final NicknamePolicy nicknamePolicy;

    @Transactional
    public void createInitialData(Account account, String playerNickname) {
        PlayerProfile profile = playerProfileRepository.save(new PlayerProfile(account, playerNickname));
        Long playerId = profile.getPlayerId();

        AccountConstData accountConst = staticDataService.getAccountConst();
        currencyRepository.save(new Currency(playerId, accountConst.initialGold(), accountConst.initialGem(),
                accountConst.initialStamina(), Instant.now(clock)));
        stageProgressRepository.save(new StageProgress(playerId, staticDataService.getFirstStageId()));
        playerStatRepository.save(new PlayerStat(playerId));
        grantStarterEquipment(playerId);
        // 랭킹값 변경알림(Redis에 갱신)
        eventPublisher.publishEvent(new RankingChangedEvent(playerId));
    }

    // 신규 계정 전용: 시작 무기를 지급하고 바로 장착한다. 기존 계정에는 적용하지 않는다
    private void grantStarterEquipment(Long playerId) {
        ItemData starterItem = staticDataService.getItems().get(STARTER_WEAPON_ITEM_ID);
        if (starterItem == null) {
            throw new IllegalStateException("시작 장비 아이템이 정적 데이터에 없습니다: " + STARTER_WEAPON_ITEM_ID);
        }
        Equipment starter = Equipment.create(playerId, starterItem, ItemGrade.COMMON);   // 시작 무기는 Common
        starter.equip();
        equipmentRepository.save(starter);
    }

    @Transactional(readOnly = true)
    public PlayerSaveDataResponse loadSave(Long playerId) {
        PlayerProfile profile = getProfile(playerId);
        Currency currency = currencyRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "재화 데이터가 없습니다."));
        StageProgress stageProgress = getStageProgress(playerId);
        PlayerStat playerStat = getPlayerStat(playerId);
        List<StageRecordResponse> stageRecords = stageRecordRepository.findByPlayerIdOrderByStageIdAsc(playerId)
                .stream()
                .map(StageRecordResponse::new)
                .toList();
        List<Integer> claimedChapterChests = chapterChestClaimRepository.findByPlayerIdOrderByStageIdAsc(playerId)
                .stream()
                .map(ChapterChestClaim::getStageId)
                .toList();

        // 회복한 스태미나는 응답에만 반영한다. 엔티티를 바꾸면, save 트랜잭션 안에서 불릴 때
        // 재화 행 전체가 읽은 시점 값으로 다시 저장되어 동시에 처리된 구매·전투 결과를 덮어쓴다
        AccountConstData accountConst = staticDataService.getAccountConst();
        Currency.EnergyState energy = currency.energyAt(
                Instant.now(clock), accountConst.maxStamina(), accountConst.staminaRecoverySeconds());

        return new PlayerSaveDataResponse(
                new PlayerProfileResponse(profile),
                new CurrencyResponse(currency, energy),
                new StageProgressResponse(stageProgress),
                new PlayerStatResponse(playerStat),
                stageRecords,
                claimedChapterChests
        );
    }

    // 세이브: 현재 스테이지 선택만 반영한다.
    // 닉네임 변경은 전용 API에서 처리한다.
    @Transactional
    public PlayerSaveDataResponse save(Long playerId, PlayerSaveRequest request) {
        // 전투·상점·진화·인벤토리와 같은 플레이어 단위 잠금.
        // 프로필과 진행도를 저장하는 동안 전투 결과가 끼어들어 경험치나 최고 클리어가 옛 값으로 되돌아가지 않게 한다
        Currency currency = currencyRepository.findForUpdate(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "재화 데이터가 없습니다."));
        PlayerProfile profile = getProfile(playerId);
        StageProgress stageProgress = getStageProgress(playerId);

        recordIgnoredValueMismatch(playerId, request, profile, currency, stageProgress);

        // 닉네임은 전용 API에서만 변경한다.
        // 기존 클라이언트 호환을 위해 요청 필드는 유지하지만,
        // 이 API에서는 전달된 닉네임을 저장하지 않는다.

        // 서버에 저장된 진행도로 스테이지 해금 여부 판정
        int currentStageId = request.getStageProgress().getCurrentStageId();

        if (!stageUnlockPolicy.isUnlocked(currentStageId, stageProgress.getMaxClearedStageId())) {
            suspiciousActivityRecorder.record(
                    playerId,
                    SuspiciousActivityType.SAVE_LOCKED_STAGE,
                        "currentStageId=" + currentStageId +
                            " serverMaxClearedStageId=" + stageProgress.getMaxClearedStageId()
            );

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해금되지 않은 스테이지입니다.");
        }

        stageProgress.selectStage(currentStageId);

        return loadSave(playerId);
    }

    // 무시하는 필드가 서버 값과 다르면 의심 활동으로 기록한다. 요청은 거부하지 않는다.
    // 보내지 않은 필드(null)는 비교하지 않는다
    @SuppressWarnings("deprecation")
    private void recordIgnoredValueMismatch(Long playerId, PlayerSaveRequest request, PlayerProfile profile,
                                            Currency currency, StageProgress stageProgress) {
        List<String> mismatches = new ArrayList<>();

        ProfileSaveRequest profileRequest = request.getProfile();
        addIfDifferent(mismatches, "accountLevel", profileRequest.getAccountLevel(), profile.getAccountLevel());
        addIfDifferent(mismatches, "accountExp", profileRequest.getAccountExp(), profile.getAccountExp());

        CurrencySaveRequest currencyRequest = request.getCurrency();
        if (currencyRequest != null) {
            addIfDifferent(mismatches, "currencyGold", currencyRequest.getCurrencyGold(), currency.getCurrencyGold());
            addIfDifferent(mismatches, "currencyGem", currencyRequest.getCurrencyGem(), currency.getCurrencyGem());

            // 스태미나는 시간이 지나면 서버 쪽이 회복되므로, 클라이언트 값이 더 높을 때만 기록한다
            AccountConstData accountConst = staticDataService.getAccountConst();
            int serverEnergy = currency.energyAt(Instant.now(clock),
                    accountConst.maxStamina(), accountConst.staminaRecoverySeconds()).energy();
            Integer clientEnergy = currencyRequest.getCurrencyEnergy();
            if (clientEnergy != null && clientEnergy > serverEnergy) {
                mismatches.add("currencyEnergy client=" + clientEnergy + " server=" + serverEnergy);
            }
        }

        Integer clientMaxCleared = request.getStageProgress().getMaxClearedStageId();
        if (clientMaxCleared != null && !clientMaxCleared.equals(stageProgress.getMaxClearedStageId())) {
            mismatches.add("maxClearedStageId client=" + clientMaxCleared
                    + " server=" + stageProgress.getMaxClearedStageId());
        }

        PlayerStatSaveRequest statRequest = request.getPlayerStat();
        if (statRequest != null) {
            PlayerStat playerStat = getPlayerStat(playerId);
            addIfDifferent(mismatches, "playerStatAttackLevel",
                    statRequest.getPlayerStatAttackLevel(), playerStat.getPlayerStatAttackLevel());
            addIfDifferent(mismatches, "playerStatHpLevel",
                    statRequest.getPlayerStatHpLevel(), playerStat.getPlayerStatHpLevel());
            addIfDifferent(mismatches, "playerStatDefenseLevel",
                    statRequest.getPlayerStatDefenseLevel(), playerStat.getPlayerStatDefenseLevel());
        }

        if (!mismatches.isEmpty()) {
            suspiciousActivityRecorder.record(playerId, SuspiciousActivityType.SAVE_VALUE_MISMATCH,
                    String.join(", ", mismatches));
        }
    }

    private static void addIfDifferent(List<String> mismatches, String field, Integer client, int server) {
        if (client != null && client != server) {
            mismatches.add(field + " client=" + client + " server=" + server);
        }
    }

    private PlayerProfile getProfile(Long playerId) {
        return playerProfileRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "플레이어 데이터가 없습니다."));
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

    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(Long playerId, String nickname) {

        PlayerProfile profile = getProfile(playerId);

        // 현재 사용 중인 닉네임으로 변경하는 것은 허용하지 않는다
        if (nickname != null &&
                nickname.equalsIgnoreCase(profile.getPlayerNickname())) {
            return false;
        }

        // 닉네임 규칙 검사
        nicknamePolicy.validate(nickname);

        // 대소문자를 구분하지 않고 중복 검사
        return !playerProfileRepository
                .existsByPlayerNicknameIgnoreCase(nickname);
    }

}