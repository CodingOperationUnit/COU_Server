package com.couserver.gamedata;

import com.couserver.TestcontainersConfiguration;
import com.couserver.gamedata.dto.SkillData;
import com.couserver.gamedata.repository.GameDataVersionRepository;
import com.couserver.gamedata.repository.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

// 스킬 데이터 테스트 공통 설정. 모든 하위 클래스가 같은 스프링 컨텍스트(같은 MySQL 컨테이너)를 쓴다.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
abstract class GameDataIntegrationTest {

    @Autowired
    protected SkillRepository skillRepository;

    @Autowired
    protected GameDataVersionRepository gameDataVersionRepository;

    // 서버 시작 때 Skill.json이 이미 반영돼 있으므로, 테스트마다 "한 번도 반영 안 된" 상태로 되돌린다
    @BeforeEach
    void resetSkillData() {
        skillRepository.deleteAllInBatch();
        gameDataVersionRepository.deleteAllInBatch();
    }

    // 명세 예시와 같은 스킬 두 개
    protected static SkillData shuriken() {
        return new SkillData(20010, "Shuriken", 0, "Nearest", 1.0f, 4.0f, 1.0f, 5.0f, "수리검",
                "레벨1 설명", "레벨2 설명", "레벨3 설명", "레벨4 설명", "레벨5 설명");
    }

    protected static SkillData keyboardSwitch() {
        return new SkillData(21010, "KeyboardSwitch", 1, "None", 0.0f, 0.0f, 0.1f, 0.0f, "기계식 축",
                "", "", "", "", "");
    }

    protected static SkillData skill(int skillId, int skillCategory, String skillType) {
        return new SkillData(skillId, "Skill" + skillId, skillCategory, skillType, 1.0f, 1.0f, 1.0f, 1.0f, "설명",
                "", "", "", "", "");
    }

    protected static SkillData withDamage(SkillData s, float skillDamage) {
        return new SkillData(s.skillId(), s.skillName(), s.skillCategory(), s.skillType(),
                s.skillCooldown(), s.skillSpeed(), skillDamage, s.skillRange(), s.skillDescription(),
                s.level1SkillDescription(), s.level2SkillDescription(), s.level3SkillDescription(),
                s.level4SkillDescription(), s.level5SkillDescription());
    }

    protected static SkillData withName(SkillData s, String skillName) {
        return new SkillData(s.skillId(), skillName, s.skillCategory(), s.skillType(),
                s.skillCooldown(), s.skillSpeed(), s.skillDamage(), s.skillRange(), s.skillDescription(),
                s.level1SkillDescription(), s.level2SkillDescription(), s.level3SkillDescription(),
                s.level4SkillDescription(), s.level5SkillDescription());
    }
}
