package com.couserver.gamedata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.couserver.gamedata.dto.SkillsResponse;
import com.couserver.gamedata.service.GameDataService;
import com.couserver.gamedata.service.SkillDataLoader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

// Skill.json 원문(JSON)을 읽는 단계의 검증: 필드 누락, 타입 오류 등
class SkillDataLoaderTest extends GameDataIntegrationTest {

    private static final String VALID_JSON = """
            {
              "datas": [
                {
                  "skillId": 20010,
                  "skillName": "Shuriken",
                  "skillCategory": 0,
                  "skillType": "Nearest",
                  "skillCooldown": 1.0,
                  "skillSpeed": 4.0,
                  "skillDamage": 1.0,
                  "skillRange": 5.0,
                  "skillDescription": "수리검",
                  "level1SkillDescription": "레벨1 설명",
                  "level2SkillDescription": "레벨2 설명",
                  "level3SkillDescription": "레벨3 설명",
                  "level4SkillDescription": "레벨4 설명",
                  "level5SkillDescription": "레벨5 설명"
                }
              ]
            }
            """;

    @Autowired
    private SkillDataLoader skillDataLoader;

    @Autowired
    private GameDataService gameDataService;

    @Autowired
    private ObjectMapper objectMapper;

    private SkillsResponse baseline;

    // 기존 데이터(버전 1)가 있는 상태에서 시작한다
    @BeforeEach
    void loadBaseline() {
        skillDataLoader.load(stream(VALID_JSON));
        baseline = gameDataService.getSkills();
        assertThat(baseline.version()).isEqualTo(1);
    }

    @Test
    @DisplayName("레포의 data/Skill.json은 검증을 통과한다")
    void repositoryFile_isValid() throws Exception {
        try (InputStream in = new ClassPathResource("data/Skill.json").getInputStream()) {
            assertThatCode(() -> skillDataLoader.load(in)).doesNotThrowAnyException();
        }
    }

    @ParameterizedTest(name = "{0} 누락")
    @ValueSource(strings = {
            "skillId", "skillName", "skillCategory", "skillType",
            "skillCooldown", "skillSpeed", "skillDamage", "skillRange",
            "skillDescription",
            "level1SkillDescription", "level2SkillDescription", "level3SkillDescription",
            "level4SkillDescription", "level5SkillDescription"})
    @DisplayName("필드가 하나라도 없으면 거부하고 기존 데이터를 유지한다")
    void missingField_rejected(String field) {
        assertRejected(modify(row -> row.remove(field)));
    }

    @ParameterizedTest(name = "{0} = null")
    @ValueSource(strings = {"skillId", "skillName", "skillCooldown", "level1SkillDescription"})
    @DisplayName("필드 값이 null이면 거부한다")
    void nullField_rejected(String field) {
        assertRejected(modify(row -> row.putNull(field)));
    }

    @Test
    @DisplayName("숫자 필드에 문자열이 오면 거부한다 (\"20010\")")
    void stringForInt_rejected() {
        assertRejected(modify(row -> row.put("skillId", "20010")));
        assertRejected(modify(row -> row.put("skillCooldown", "1.0")));
    }

    @Test
    @DisplayName("정수 필드에 소수가 오면 거부한다 (1.5)")
    void floatForInt_rejected() {
        assertRejected(modify(row -> row.put("skillCategory", 0.5)));
    }

    @Test
    @DisplayName("문자열 필드에 숫자가 오면 거부한다")
    void numberForString_rejected() {
        assertRejected(modify(row -> row.put("skillName", 123)));
    }

    @Test
    @DisplayName("datas가 없거나 JSON이 깨졌으면 거부한다")
    void brokenFile_rejected() {
        assertRejected("{}");
        assertRejected("{\"datas\": [");
        assertRejected("");
    }

    @Test
    @DisplayName("float 필드에 정수(1)를 써도 받아들인다")
    void intForFloat_accepted() {
        skillDataLoader.load(stream(modify(row -> row.put("skillCooldown", 2))));

        assertThat(gameDataService.getSkills().datas().getFirst().skillCooldown()).isEqualTo(2.0f);
    }

    @Test
    @DisplayName("명세에 없는 열(클라이언트 전용 등)은 무시하고 응답에도 넣지 않는다")
    void unknownField_ignored() {
        skillDataLoader.load(stream(modify(row -> row.put("iconPath", "Skill/Icon/Shuriken"))));

        assertThat(gameDataService.getSkills()).isEqualTo(baseline); // 내용 같음 -> 버전도 1 그대로
    }

    @Test
    @DisplayName("줄바꿈·공백만 다르면 버전이 오르지 않는다")
    void whitespaceOnly_versionUnchanged() {
        skillDataLoader.load(stream(VALID_JSON.replace("\n", "\r\n").replace("  ", "\t")));

        assertThat(gameDataService.getVersions().skill()).isEqualTo(1);
    }

    private void assertRejected(String json) {
        assertThatThrownBy(() -> skillDataLoader.load(stream(json)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(gameDataService.getSkills()).isEqualTo(baseline);
    }

    // VALID_JSON의 첫 행을 고친 JSON
    private String modify(Consumer<ObjectNode> change) {
        ObjectNode root = (ObjectNode) objectMapper.readTree(VALID_JSON);
        change.accept((ObjectNode) root.get("datas").get(0));
        return objectMapper.writeValueAsString(root);
    }

    private static InputStream stream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}
