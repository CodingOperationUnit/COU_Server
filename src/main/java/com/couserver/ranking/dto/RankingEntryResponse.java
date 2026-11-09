package com.couserver.ranking.dto;

public record RankingEntryResponse(int rank, Long playerId, String accountLoginId, String nickname, int accountLevel, int value) {
    public RankingEntryResponse(int rank, RankingRow row) {
        this(rank, row.playerId(), row.accountLoginId(), row.nickname(), row.accountLevel(), row.value());
    }
}