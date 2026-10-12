package com.couserver.ranking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// 서버가 다 뜬 직후 한 번 실행된다
@Component
@RequiredArgsConstructor
public class RankingRedisInitializer implements ApplicationRunner {
    private final RankingRedisSyncService syncService;

    @Override
    public void run(ApplicationArguments args) {
        syncService.rebuildAll();
    }
}