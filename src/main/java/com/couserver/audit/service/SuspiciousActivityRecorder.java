package com.couserver.audit.service;

import com.couserver.audit.entity.SuspiciousActivity;
import com.couserver.audit.entity.SuspiciousActivityType;
import com.couserver.audit.repository.SuspiciousActivityRepository;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

// 의심 활동을 WARN 로그로 남기고, 전용 스레드에서 DB에 저장한다.
// - 요청이 400으로 롤백되어도 기록은 남는다 (요청 트랜잭션과 무관한 스레드에서 저장)
// - 요청 하나가 DB 연결을 두 개 잡지 않는다 (REQUIRES_NEW를 쓰면 의심 요청이 몰릴 때 연결 풀이 바닥날 수 있다)
// - 기록 실패나 대기열 초과는 원래 요청에 영향을 주지 않는다
@Slf4j
@Component
public class SuspiciousActivityRecorder {
    private static final int MAX_DETAIL_LENGTH = 500;
    private static final int QUEUE_CAPACITY = 1000;

    private final SuspiciousActivityRepository repository;
    private final Clock clock;
    private final ThreadPoolExecutor executor;

    public SuspiciousActivityRecorder(SuspiciousActivityRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
        this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                runnable -> {
                    Thread thread = new Thread(runnable, "suspicious-activity-recorder");
                    thread.setDaemon(true);
                    return thread;
                },
                (runnable, pool) -> log.warn("의심 활동 기록 대기열이 가득 찼거나 종료 중이라 DB 기록을 건너뜁니다."));
    }

    public void record(Long playerId, SuspiciousActivityType type, String detail) {
        String trimmed = truncate(detail);
        Instant now = Instant.now(clock);
        log.warn("[의심 활동] playerId={} type={} detail={}", playerId, type, trimmed);
        executor.execute(() -> save(playerId, type, trimmed, now));
    }

    private void save(Long playerId, SuspiciousActivityType type, String detail, Instant createdAt) {
        try {
            repository.save(new SuspiciousActivity(playerId, type, detail, createdAt));
        } catch (Exception e) {
            log.warn("의심 활동 DB 기록 실패 playerId={} type={}", playerId, type, e);
        }
    }

    private static String truncate(String detail) {
        if (detail == null) {
            return "";
        }
        return detail.length() <= MAX_DETAIL_LENGTH ? detail : detail.substring(0, MAX_DETAIL_LENGTH);
    }

    @PreDestroy
    void shutdown() throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            log.warn("의심 활동 기록 {}건을 저장하지 못하고 종료합니다.", executor.getQueue().size());
            executor.shutdownNow();
        }
    }
}