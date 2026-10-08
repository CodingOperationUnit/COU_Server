package com.couserver.gamedata.service;

import com.couserver.gamedata.dto.SkillDataFile;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

// 앱 시작 시 data/Skill.json을 읽어 DB에 반영한다.
// 형식이나 검증이 틀리면 예외를 던져 서버가 시작하지 않는다. DB의 기존 데이터는 그대로 남는다.
@Slf4j
@Component
public class SkillDataLoader implements ApplicationRunner {

    private static final String FILE_PATH = "data/Skill.json";

    private final SkillDataService skillDataService;
    private final ObjectMapper strictMapper;

    public SkillDataLoader(SkillDataService skillDataService, ObjectMapper objectMapper) {
        this.skillDataService = skillDataService;
        // 필드가 없거나 null이면 실패하고, "20010" -> 20010 / 1.5 -> 1 / 123 -> "123" 같은 자동 변환을 막는다.
        // 클라이언트 전용 열처럼 모르는 필드는 기존 설정대로 무시한다.
        this.strictMapper = objectMapper.rebuild()
                .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .withCoercionConfig(LogicalType.Textual, cfg -> cfg
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
                .build();
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        try (InputStream in = new ClassPathResource(FILE_PATH).getInputStream()) {
            load(in);
        }
    }

    public void load(InputStream in) {
        SkillDataFile file;
        try {
            file = strictMapper.readValue(in, SkillDataFile.class);
        } catch (JacksonException e) {
            throw new IllegalStateException(FILE_PATH + " 형식이 올바르지 않습니다: " + e.getOriginalMessage(), e);
        }
        skillDataService.apply(file.datas());
    }
}
