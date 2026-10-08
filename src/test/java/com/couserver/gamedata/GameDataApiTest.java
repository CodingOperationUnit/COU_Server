package com.couserver.gamedata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.couserver.gamedata.service.SkillDataService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// 두 GET API의 응답 구조가 명세와 정확히 같은지 확인한다. 요청에는 인증 헤더를 넣지 않는다.
class GameDataApiTest extends GameDataIntegrationTest {

    private static final List<String> SKILL_FIELDS = List.of(
            "skillId", "skillName", "skillCategory", "skillType",
            "skillCooldown", "skillSpeed", "skillDamage", "skillRange",
            "skillDescription",
            "level1SkillDescription", "level2SkillDescription", "level3SkillDescription",
            "level4SkillDescription", "level5SkillDescription");
    private static final List<String> INT_FIELDS = List.of("skillId", "skillCategory");
    private static final List<String> FLOAT_FIELDS = List.of("skillCooldown", "skillSpeed", "skillDamage", "skillRange");
    private static final List<String> STRING_FIELDS = List.of(
            "skillName", "skillType", "skillDescription",
            "level1SkillDescription", "level2SkillDescription", "level3SkillDescription",
            "level4SkillDescription", "level5SkillDescription");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SkillDataService skillDataService;

    @Test
    @DisplayName("versions: 인증 없이 200, 본문은 skill 하나만 가진다")
    void versions_structure() throws Exception {
        skillDataService.apply(List.of(shuriken(), keyboardSwitch()));

        String body = getJson("/api/v1/data/versions");

        assertThat(body).isEqualTo("{\"skill\":1}");
    }

    @Test
    @DisplayName("versions/skills: 반영된 적이 없으면 버전 0, 빈 목록")
    void empty_data() throws Exception {
        assertThat(getJson("/api/v1/data/versions")).isEqualTo("{\"skill\":0}");
        assertThat(getJson("/api/v1/data/skills")).isEqualTo("{\"version\":0,\"datas\":[]}");
    }

    @Test
    @DisplayName("skills: 최상위는 version, datas 두 키뿐이다")
    void skills_topLevel() throws Exception {
        skillDataService.apply(List.of(shuriken(), keyboardSwitch()));

        JsonNode root = objectMapper.readTree(getJson("/api/v1/data/skills"));

        assertThat(root.propertyNames()).containsExactlyInAnyOrder("version", "datas");
        assertThat(root.get("version").isInt()).isTrue();
        assertThat(root.get("version").intValue()).isEqualTo(1);
        assertThat(root.get("datas").isArray()).isTrue();
        assertThat(root.get("datas").size()).isEqualTo(2);
    }

    @Test
    @DisplayName("skills: 각 항목은 명세의 14개 필드를 정확한 타입으로 가진다")
    void skills_itemFieldsAndTypes() throws Exception {
        skillDataService.apply(List.of(shuriken(), keyboardSwitch()));

        JsonNode datas = objectMapper.readTree(getJson("/api/v1/data/skills")).get("datas");

        for (JsonNode item : datas) {
            assertThat(item.propertyNames()).containsExactlyInAnyOrderElementsOf(SKILL_FIELDS);
            INT_FIELDS.forEach(f -> assertThat(item.get(f).isInt()).as(f + " 는 정수").isTrue());
            FLOAT_FIELDS.forEach(f -> assertThat(item.get(f).isFloatingPointNumber()).as(f + " 는 실수(숫자)").isTrue());
            STRING_FIELDS.forEach(f -> assertThat(item.get(f).isString()).as(f + " 는 문자열(null 아님)").isTrue());
        }
    }

    @Test
    @DisplayName("skills: 값이 명세 예시와 같고, 정수 값 float는 1.0처럼 숫자로, 빈 설명은 \"\"로 나간다")
    void skills_values() throws Exception {
        skillDataService.apply(List.of(shuriken(), keyboardSwitch()));

        String body = getJson("/api/v1/data/skills");
        JsonNode datas = objectMapper.readTree(body).get("datas");

        JsonNode first = datas.get(0);
        assertThat(first.get("skillId").intValue()).isEqualTo(20010);
        assertThat(first.get("skillName").stringValue()).isEqualTo("Shuriken");
        assertThat(first.get("skillCategory").intValue()).isEqualTo(0);
        assertThat(first.get("skillType").stringValue()).isEqualTo("Nearest");
        assertThat(first.get("skillDescription").stringValue()).isEqualTo("수리검");
        assertThat(first.get("level5SkillDescription").stringValue()).isEqualTo("레벨5 설명");

        JsonNode second = datas.get(1);
        assertThat(second.get("skillId").intValue()).isEqualTo(21010);
        assertThat(second.get("skillType").stringValue()).isEqualTo("None");
        assertThat(second.get("level1SkillDescription").stringValue()).isEmpty();

        // 원문 그대로 확인: 문자열("1.0")이나 정수(1)가 아니라 실수 숫자로 나간다
        assertThat(body)
                .contains("\"skillCooldown\":1.0")
                .contains("\"skillRange\":5.0")
                .contains("\"skillCooldown\":0.0")
                .contains("\"skillDamage\":0.1")
                .contains("\"level1SkillDescription\":\"\"")
                .doesNotContain("null");
    }

    @Test
    @DisplayName("skills: 반영 순서와 상관없이 skillId 오름차순으로 내려준다")
    void skills_sortedById() throws Exception {
        skillDataService.apply(List.of(keyboardSwitch(), skill(100, 0, "Forward"), shuriken()));

        JsonNode datas = objectMapper.readTree(getJson("/api/v1/data/skills")).get("datas");

        assertThat(datas.valueStream().map(n -> n.get("skillId").intValue()))
                .containsExactly(100, 20010, 21010);
    }

    @Test
    @DisplayName("skills: float 값이 DB를 거쳐도 입력과 같은 값으로 나간다")
    void skills_floatPrecision() throws Exception {
        skillDataService.apply(List.of(withDamage(shuriken(), 1.2345678f)));

        String body = getJson("/api/v1/data/skills");

        assertThat(body).contains("\"skillDamage\":1.2345678");
    }

    @Test
    @DisplayName("versions의 skill 값과 skills의 version 값은 항상 같다")
    void versions_matchSkillsVersion() throws Exception {
        skillDataService.apply(List.of(shuriken()));
        assertVersionsMatch(1);

        skillDataService.apply(List.of(shuriken(), keyboardSwitch()));
        assertVersionsMatch(2);

        skillDataService.apply(List.of(keyboardSwitch(), shuriken())); // 순서만 다름 -> 그대로
        assertVersionsMatch(2);
    }

    private void assertVersionsMatch(int expected) throws Exception {
        int fromVersions = objectMapper.readTree(getJson("/api/v1/data/versions")).get("skill").intValue();
        int fromSkills = objectMapper.readTree(getJson("/api/v1/data/skills")).get("version").intValue();
        assertThat(fromVersions).isEqualTo(expected);
        assertThat(fromSkills).isEqualTo(expected);
    }

    // 인증 헤더 없이 호출한다
    private String getJson(String path) throws Exception {
        return mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }
}
