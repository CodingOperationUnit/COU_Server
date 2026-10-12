package com.couserver.ranking.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class RankingRedisRepository {
    public static final String ACCOUNT_LEVEL_KEY = "ranking:account-level";
    public static final String MAX_STAGE_KEY = "ranking:max-stage";
    private static final String STAGE_SURVIVAL_PREFIX = "ranking:stage-survival:";

    private static final long ID_RANGE = 10_000_000L;

    private final StringRedisTemplate redis;

    // Redis에서 꺼낸 한 줄: 누구, 값
    public record Member(long playerId, long value) {}

    public static String stageSurvivalKey(int stageId) {
        return STAGE_SURVIVAL_PREFIX + stageId;
    }

    // 점수 추가/갱신 (이미 있으면 덮어쓴다)
    public void put(String key, long playerId, long value) {
        redis.opsForZSet().add(key, String.valueOf(playerId), toScore(value, playerId));
    }

    // 상위 size명
    public List<Member> top(String key, int size) {
        Set<TypedTuple<String>> tuples = redis.opsForZSet().reverseRangeWithScores(key, 0, size - 1);
        List<Member> result = new ArrayList<>();
        if (tuples == null) return result;
        for (TypedTuple<String> t : tuples) {
            result.add(new Member(Long.parseLong(t.getValue()), toValue(t.getScore())));
        }
        return result;
    }

    // 내 순위
    public Integer rank(String key, long playerId) {
        Long rank = redis.opsForZSet().reverseRank(key, String.valueOf(playerId));
        return rank == null ? null : (int) (rank + 1);
    }

    // 내 값. 기록이 없으면 null
    public Long value(String key, long playerId) {
        Double score = redis.opsForZSet().score(key, String.valueOf(playerId));
        return score == null ? null : toValue(score);
    }


    public void clear(String key) {
        redis.delete(key);
    }

    private static double toScore(long value, long playerId) {
        return (double) (value * ID_RANGE + (ID_RANGE - 1 - playerId));
    }

    private static long toValue(double score) {
        return (long) score / ID_RANGE;
    }
}
