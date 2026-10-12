package com.couserver.ranking.dto;

import java.util.List;

public record RankingResponse(String type, Integer stageId, List<RankingEntryResponse> entries,
                              Integer myRank, Integer myValue) {}