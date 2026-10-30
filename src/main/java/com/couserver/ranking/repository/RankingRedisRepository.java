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
    public static final String COMBAT_POWER_KEY = "ranking:combat-power";
    private static final String STAGE_SURVIVAL_PREFIX = "ranking:stage-survival:";
    private static final long EXP_RANGE = 1_000_000L;

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

    // 계정 레벨 랭킹의 value: 레벨이 우선, 같으면 경험치
    public static long accountLevelValue(int level, int exp) {
        return level * EXP_RANGE + exp;
    }

    // value에서 레벨만 꺼내기 (화면에 보여줄 값)
    public static int levelOf(long value) {
        return (int) (value / EXP_RANGE);
    }

    // ranking: 으로 시작하는 키 전부 삭제 (백필 전에 비우기)
    public void clearAll() {
        Set<String> keys = redis.keys("ranking:*");
        if (keys != null && !keys.isEmpty()) redis.delete(keys);
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
