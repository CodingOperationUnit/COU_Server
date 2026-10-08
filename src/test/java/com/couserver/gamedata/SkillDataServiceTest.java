package com.couserver.gamedata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.couserver.gamedata.dto.SkillData;
import com.couserver.gamedata.dto.SkillsResponse;
import com.couserver.gamedata.service.GameDataService;
import com.couserver.gamedata.service.SkillDataService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

// 버전 증가 조건과, 검증 실패 시 기존 데이터·버전이 그대로 남는지 확인한다
class SkillDataServiceTest extends GameDataIntegrationTest {

    @Autowired
    private SkillDataService skillDataService;

    @Autowired
    private GameDataService gameDataService;

    // ---------- 버전 증가 조건 ----------

    @Test
    @DisplayName("처음 반영하면 버전 1")
    void firstApply_version1() {
        skillDataService.apply(List.of(shuriken(), keyboardSwitch()));

        assertThat(version()).isEqualTo(1);
        assertThat(skillRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("같은 내용을 다시 반영하면 버전이 그대로")
    void sameContent_versionUnchanged() {
        skillDataService.apply(List.of(shuriken(), keyboardSwitch()));
        skillDataService.apply(List.of(shuriken(), keyboardSwitch()));
        skillDataService.apply(List.of(shuriken(), keyboardSwitch()));

        assertThat(version()).isEqualTo(1);
    }

    @Test
    @DisplayName("행 순서만 바뀌면 버전이 그대로")
    void reordered_versionUnchanged() {
        skillDataService.apply(List.of(shuriken(), keyboardSwitch()));
        skillDataService.apply(List.of(keyboardSwitch(), shuriken()));

        assertThat(version()).isEqualTo(1);
    }

    @Test
    @DisplayName("값 하나가 바뀌면 버전 +1, 바뀐 값이 조회된다")
    void valueChanged_versionIncreased() {
        skillDataService.apply(List.of(shuriken(), keyboardSwitch()));
        skillDataService.apply(List.of(withDamage(shuriken(), 1.5f), keyboardSwitch()));

        assertThat(version()).isEqualTo(2);
        assertThat(gameDataService.getSkills().datas().getFirst().skillDamage()).isEqualTo(1.5f);
    }

    @Test
    @DisplayName("문자열 값이 바뀌어도 버전 +1")
    void stringChanged_versionIncreased() {
        skillDataService.apply(List.of(shuriken()));
        skillDataService.apply(List.of(withName(shuriken(), "Shuriken2")));

        assertThat(version()).isEqualTo(2);
    }

    @Test
    @DisplayName("행이 추가되거나 삭제되면 버전 +1, 삭제된 행은 조회되지 않는다")
    void rowAddedOrRemoved_versionIncreased() {
        skillDataService.apply(List.of(shuriken()));
        skillDataService.apply(List.of(shuriken(), keyboardSwitch()));
        assertThat(version()).isEqualTo(2);

        skillDataService.apply(List.of(keyboardSwitch()));
        assertThat(version()).isEqualTo(3);
        assertThat(skillRepository.existsById(20010)).isFalse();
        assertThat(gameDataService.getSkills().datas()).hasSize(1);
    }

    @Test
    @DisplayName("바뀔 때마다 1씩만 오른다")
    void increasesByOne() {
        skillDataService.apply(List.of(shuriken()));
        skillDataService.apply(List.of(withDamage(shuriken(), 2f)));
        skillDataService.apply(List.of(withDamage(shuriken(), 2f))); // 같음
        skillDataService.apply(List.of(withDamage(shuriken(), 3f)));

        assertThat(version()).isEqualTo(3);
    }

    // ---------- 검증 실패 시 기존 데이터 유지 ----------

    static Stream<Arguments> invalidRows() {
        return Stream.of(
                Arguments.of("skillId 0", List.of(skill(0, 0, "Nearest"))),
                Arguments.of("skillId 음수", List.of(skill(-1, 0, "Nearest"))),
                Arguments.of("skillId 중복", List.of(skill(100, 0, "Nearest"), skill(100, 1, "None"))),
                Arguments.of("skillCategory 2", List.of(skill(100, 2, "Nearest"))),
                Arguments.of("skillCategory -1", List.of(skill(100, -1, "Nearest"))),
                Arguments.of("skillType 오타", List.of(skill(100, 0, "Backward"))),
                Arguments.of("skillType 대소문자 다름", List.of(skill(100, 0, "nearest"))),
                Arguments.of("skillType 빈 문자열", List.of(skill(100, 0, ""))),
                Arguments.of("skillType null", List.of(skill(100, 0, null))),
                Arguments.of("문자열 필드 null", List.of(withName(skill(100, 0, "None"), null))),
                Arguments.of("빈 목록", List.of()),
                Arguments.of("목록 null", null),
                Arguments.of("행 null", Arrays.asList(skill(100, 0, "None"), null)),
                // 정상 행이 섞여 있어도 하나라도 틀리면 전체 거부
                Arguments.of("정상 행 + 잘못된 행", List.of(withDamage(shuriken(), 9f), skill(100, 5, "None"))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRows")
    @DisplayName("검증에 실패하면 예외를 던지고 기존 데이터와 버전을 그대로 둔다")
    void invalid_keepsExistingData(String caseName, List<SkillData> rows) {
        skillDataService.apply(List.of(shuriken(), keyboardSwitch()));
        SkillsResponse before = gameDataService.getSkills();

        assertThatThrownBy(() -> skillDataService.apply(rows))
                .isInstanceOf(IllegalStateException.class);

        assertThat(gameDataService.getSkills()).isEqualTo(before);
        assertThat(version()).isEqualTo(1);
    }

    @Test
    @DisplayName("반영된 적이 없을 때 검증에 실패하면 아무것도 만들지 않는다")
    void invalid_onEmptyDb() {
        assertThatThrownBy(() -> skillDataService.apply(List.of(skill(0, 0, "None"))))
                .isInstanceOf(IllegalStateException.class);

        assertThat(skillRepository.count()).isZero();
        assertThat(gameDataVersionRepository.count()).isZero();
    }

    @Test
    @DisplayName("오류 메시지에 문제가 된 skillId가 나온다")
    void invalid_messageHasId() {
        List<SkillData> rows = new ArrayList<>(List.of(shuriken(), skill(21099, 3, "None")));

        assertThatThrownBy(() -> skillDataService.apply(rows))
                .hasMessageContaining("21099")
                .hasMessageContaining("skillCategory");
    }

    private int version() {
        return gameDataService.getVersions().skill();
    }
}
