package com.couserver.gamedata.service;

import com.couserver.gamedata.dto.SkillData;
import com.couserver.gamedata.entity.GameDataVersion;
import com.couserver.gamedata.entity.Skill;
import com.couserver.gamedata.repository.GameDataVersionRepository;
import com.couserver.gamedata.repository.SkillRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

// 스킬 데이터를 검증하고 DB에 반영한다.
// 검증에 하나라도 실패하면 아무것도 바꾸지 않고 예외를 던진다.
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillDataService {

    public static final String DATA_TYPE = "skill";
    private static final Set<String> SKILL_TYPES = Set.of("Nearest", "Forward", "None");

    private final SkillRepository skillRepository;
    private final GameDataVersionRepository gameDataVersionRepository;
    private final ObjectMapper objectMapper;

    // 내용이 바뀌었을 때만 전체 행을 교체하고 버전을 1 올린다
    @Transactional
    public void apply(List<SkillData> rows) {
        validate(rows);

        // 행 순서만 바뀐 경우에는 버전이 오르지 않도록 skillId 순으로 정렬해 해시를 구한다
        List<SkillData> sorted = rows.stream()
                .sorted(Comparator.comparingInt(SkillData::skillId))
                .toList();
        String hash = calculateHash(sorted);

        GameDataVersion version = gameDataVersionRepository.findWithLockByDataType(DATA_TYPE).orElse(null);

        // 내용이 같고 데이터도 그대로 있으면 아무것도 하지 않는다
        if (version != null && version.isSameContent(hash) && skillRepository.count() == sorted.size()) {
            log.info("[{}] 변경 없음 (version {})", DATA_TYPE, version.getVersion());
            return;
        }

        skillRepository.deleteAllInBatch();
        skillRepository.saveAll(sorted.stream().map(Skill::from).toList());

        if (version == null) {
            gameDataVersionRepository.save(GameDataVersion.create(DATA_TYPE, hash));
            log.info("[{}] 최초 반영 (version 1, {}개)", DATA_TYPE, sorted.size());
        } else {
            version.increaseVersion(hash);
            log.info("[{}] 변경 반영 (version {}, {}개)", DATA_TYPE, version.getVersion(), sorted.size());
        }
    }

    private static void validate(List<SkillData> rows) {
        require(rows != null && !rows.isEmpty(), "Skill 데이터 목록이 비어 있습니다.");

        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            SkillData row = rows.get(i);
            require(row != null, "Skill datas[" + i + "] 행이 비어 있습니다.");

            int id = row.skillId();
            require(id >= 1, "Skill ID가 올바르지 않습니다: " + id);
            require(seen.add(id), "중복된 Skill ID: " + id);
            require(row.skillCategory() == 0 || row.skillCategory() == 1,
                    "Skill " + id + "의 skillCategory는 0 또는 1이어야 합니다: " + row.skillCategory());
            require(row.skillType() != null && SKILL_TYPES.contains(row.skillType()),
                    "Skill " + id + "의 skillType은 Nearest, Forward, None 중 하나여야 합니다: " + row.skillType());
            require(Stream.of(row.skillName(), row.skillDescription(),
                            row.level1SkillDescription(), row.level2SkillDescription(), row.level3SkillDescription(),
                            row.level4SkillDescription(), row.level5SkillDescription())
                            .allMatch(s -> s != null),
                    "Skill " + id + "에 비어 있는(null) 문자열 필드가 있습니다.");
        }
    }

    // 파일 원문이 아니라 읽어 들인 값을 다시 JSON으로 만들어 해시를 구한다.
    // 줄바꿈(CRLF/LF)이나 공백만 바뀐 경우에는 버전이 오르지 않게 하기 위해서다.
    private String calculateHash(List<SkillData> sorted) {
        try {
            byte[] normalized = objectMapper.writeValueAsBytes(sorted);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalized);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
