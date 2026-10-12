package com.couserver.ranking.service;


import com.couserver.account.entity.PlayerProfile;
import com.couserver.account.entity.StageProgress;
import com.couserver.account.repository.PlayerProfileRepository;
import com.couserver.account.repository.StageProgressRepository;
import com.couserver.battle.entity.StageRecord;
import com.couserver.battle.repository.StageRecordRepository;
import com.couserver.common.exception.BusinessException;
import com.couserver.ranking.dto.RankingEntryResponse;
import com.couserver.ranking.dto.RankingResponse;
import com.couserver.ranking.dto.RankingRow;
import com.couserver.ranking.dto.RankingType;
import com.couserver.ranking.exception.RankingErrorCode;
import com.couserver.ranking.repository.RankingRepository;
import com.couserver.staticdata.service.StaticDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankingService {
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;

    private final RankingRepository rankingRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final StageProgressRepository stageProgressRepository;
    private final StageRecordRepository stageRecordRepository;
    private final StaticDataService staticDataService;

    public RankingResponse getRanking(Long playerId, String typeName, Integer stageId, Integer limit) {
        RankingType type = parseType(typeName);
        int size = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
        if (type == RankingType.STAGE_SURVIVAL) validateStage(stageId);

        Pageable page = PageRequest.of(0, size);
        List<RankingRow> rows = switch (type) {
            case ACCOUNT_LEVEL  -> rankingRepository.findTopByAccountLevel(page);
            case MAX_STAGE      -> rankingRepository.findTopByMaxStage(page);
            case STAGE_SURVIVAL -> rankingRepository.findTopByStageSurvival(stageId, page);
            case COMBAT_POWER   -> throw new BusinessException(RankingErrorCode.UNSUPPORTED_RANKING_TYPE);
        };

        List<RankingEntryResponse> entries = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            entries.add(new RankingEntryResponse(i + 1, rows.get(i)));
        }

        MyRanking mine = findMine(type, playerId, stageId);   // 기록 없으면 null
        return new RankingResponse(type.name(), stageId, entries,
                mine == null ? null : mine.rank(),
                mine == null ? null : mine.value());
    }

    private record MyRanking(int rank, int value) {}

    private MyRanking findMine(RankingType type, Long playerId, Integer stageId) {
        return switch (type) {
            case ACCOUNT_LEVEL -> {
                PlayerProfile p = playerProfileRepository.findById(playerId).orElse(null);
                if (p == null) yield null;
                long ahead = rankingRepository.countAheadByAccountLevel(p.getAccountLevel(), p.getAccountExp(), playerId);
                yield new MyRanking((int) ahead + 1, p.getAccountLevel());
            }
            case MAX_STAGE -> {
                StageProgress sp = stageProgressRepository.findById(playerId).orElse(null);
                if (sp == null || sp.getMaxClearedStageId() == null) yield null;
                int max = sp.getMaxClearedStageId();
                yield new MyRanking((int) rankingRepository.countAheadByMaxStage(max, playerId) + 1, max);
            }
            case STAGE_SURVIVAL -> {
                StageRecord r = stageRecordRepository.findByPlayerIdAndStageId(playerId, stageId).orElse(null);
                if (r == null || r.getBestSurvivalSeconds() <= 0) yield null;
                int sec = r.getBestSurvivalSeconds();
                yield new MyRanking((int) rankingRepository.countAheadByStageSurvival(stageId, sec, playerId) + 1, sec);
            }
            case COMBAT_POWER -> throw new BusinessException(RankingErrorCode.UNSUPPORTED_RANKING_TYPE);
        };
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
