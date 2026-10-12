package com.couserver.ranking.service;


import com.couserver.account.entity.PlayerProfile;
import com.couserver.account.repository.PlayerProfileRepository;
import com.couserver.common.exception.BusinessException;
import com.couserver.ranking.dto.RankingEntryResponse;
import com.couserver.ranking.dto.RankingResponse;
import com.couserver.ranking.dto.RankingRow;
import com.couserver.ranking.dto.RankingType;
import com.couserver.ranking.exception.RankingErrorCode;
import com.couserver.ranking.repository.RankingRedisRepository;
import com.couserver.staticdata.service.StaticDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankingService {
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;

    private final RankingRedisRepository rankingRedisRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final StaticDataService staticDataService;

    public RankingResponse getRanking(Long playerId, String typeName, Integer stageId, Integer limit) {
        RankingType type = parseType(typeName);
        int size = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
        if (type == RankingType.STAGE_SURVIVAL) validateStage(stageId);

        String key = redisKey(type, stageId);

        // 1) 순위 순서는 Redis에서
        List<RankingRedisRepository.Member> members = rankingRedisRepository.top(key, size);

        // 2) 닉네임과 레벨은 MySQL에서 한 번에
        Map<Long, PlayerProfile> profiles = playerProfileRepository
                .findAllById(members.stream().map(RankingRedisRepository.Member::playerId).toList())
                .stream()
                .collect(Collectors.toMap(PlayerProfile::getPlayerId, Function.identity()));

        List<RankingEntryResponse> entries = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            RankingRedisRepository.Member member = members.get(i);
            PlayerProfile profile = profiles.get(member.playerId());
            if (profile == null) continue;   // DB에서 삭제된 플레이어는 건너뜀
            entries.add(new RankingEntryResponse(i + 1, new RankingRow(
                    profile.getPlayerId(), profile.getPlayerNickname(), profile.getAccountLevel(),
                    displayValue(type, member.value()))));
        }

        // 3) 내 순위: 기록이 없으면 둘 다 null
        Integer myRank = rankingRedisRepository.rank(key, playerId);
        Long myValue = rankingRedisRepository.value(key, playerId);
        boolean hasMine = myRank != null && myValue != null;

        return new RankingResponse(type.name(), stageId, entries,
                hasMine ? myRank : null,
                hasMine ? Integer.valueOf(displayValue(type, myValue)) : null);
    }

    private String redisKey(RankingType type, Integer stageId) {
        return switch (type) {
            case ACCOUNT_LEVEL  -> RankingRedisRepository.ACCOUNT_LEVEL_KEY;
            case MAX_STAGE      -> RankingRedisRepository.MAX_STAGE_KEY;
            case STAGE_SURVIVAL -> RankingRedisRepository.stageSurvivalKey(stageId);
            case COMBAT_POWER   -> RankingRedisRepository.COMBAT_POWER_KEY;
        };
    }

    // 화면에 보여줄 값: 계정 레벨은 (레벨×100만+경험치)에서 레벨만 꺼낸다
    private int displayValue(RankingType type, long value) {
        return type == RankingType.ACCOUNT_LEVEL
                ? RankingRedisRepository.levelOf(value)
                : (int) value;
    }

    private RankingType parseType(String name) {
        try {
            return RankingType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(RankingErrorCode.INVALID_RANKING_TYPE);
        }
    }

    private void validateStage(Integer stageId) {
        if (stageId == null || !staticDataService.getStages().containsKey(stageId)) {
            throw new BusinessException(RankingErrorCode.STAGE_NOT_FOUND);
        }
    }
}
