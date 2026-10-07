package com.couserver.master.service;

import com.couserver.master.dto.PlayerBaseStatData;
import com.couserver.master.entity.MasterTableVersion;
import com.couserver.master.entity.PlayerBaseStat;
import com.couserver.master.repository.MasterTableVersionRepository;
import com.couserver.master.repository.PlayerBaseStatRepository;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

// 앱 시작 시 PlayerBaseStat.json을 DB에 반영한다.
// 내용이 바뀌었을 때만 데이터를 갱신하고 버전을 1 올린다.
@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerBaseStatDataLoader implements ApplicationRunner {

    public static final String TABLE_NAME = "player-base-stat";
    private static final String FILE_PATH = "data/PlayerBaseStat.json";

    private final PlayerBaseStatRepository playerBaseStatRepository;
    private final MasterTableVersionRepository masterTableVersionRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;


    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        PlayerBaseStatData data;
        try (InputStream in = new ClassPathResource(FILE_PATH).getInputStream()) {
            data = objectMapper.readValue(in, PlayerBaseStatData.class);
        }

        String hash = calculateHash(data);
        MasterTableVersion version = masterTableVersionRepository.findById(TABLE_NAME).orElse(null);

        // 내용이 같고 데이터도 있으면 아무것도 하지 않는다
        if (version != null && version.isSameContent(hash)
                && playerBaseStatRepository.existsById(PlayerBaseStat.SINGLE_ID)) {
            log.info("[{}] 변경 없음 (version {})", TABLE_NAME, version.getVersion());
            return;
        }

        saveStat(data);

        Instant now = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS);

        if (version == null) {
            masterTableVersionRepository.save(MasterTableVersion.create(TABLE_NAME, hash, now));
            log.info("[{}] 최초 반영 (version 1)", TABLE_NAME);
        } else {
            version.increaseVersion(hash, now);
            log.info("[{}] 변경 반영 (version {})", TABLE_NAME, version.getVersion());
        }
    }

    // 1행이 없으면 새로 만들고, 있으면 값만 갱신한다
    private void saveStat(PlayerBaseStatData d) {
        playerBaseStatRepository.findById(PlayerBaseStat.SINGLE_ID).ifPresentOrElse(
                stat -> stat.update(d.playerBaseAttack(), d.playerBaseHp(), d.playerBaseCriticalDamage(),
                        d.playerBaseCriticalChance(), d.playerBaseSkillDamage(), d.playerBaseMoveSpeed(),
                        d.playerBaseMaxMoveSpeed(), d.playerBaseLootRadius()),
                () -> playerBaseStatRepository.save(PlayerBaseStat.create(d.playerBaseAttack(), d.playerBaseHp(),
                        d.playerBaseCriticalDamage(), d.playerBaseCriticalChance(), d.playerBaseSkillDamage(),
                        d.playerBaseMoveSpeed(), d.playerBaseMaxMoveSpeed(), d.playerBaseLootRadius())));
    }

    // 파일 원문이 아니라 읽어 들인 값을 다시 JSON으로 만들어 해시를 구한다.
    // 줄바꿈(CRLF/LF)이나 공백만 바뀐 경우에는 버전이 오르지 않게 하기 위해서다.
    private String calculateHash(PlayerBaseStatData data) throws NoSuchAlgorithmException {
        byte[] normalized = objectMapper.writeValueAsBytes(data);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalized);
        return HexFormat.of().formatHex(digest);
    }
}