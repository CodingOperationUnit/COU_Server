package com.couserver.gamedata.service;

import com.couserver.gamedata.dto.DataVersionsResponse;
import com.couserver.gamedata.dto.SkillResponse;
import com.couserver.gamedata.dto.SkillsResponse;
import com.couserver.gamedata.entity.GameDataVersion;
import com.couserver.gamedata.repository.GameDataVersionRepository;
import com.couserver.gamedata.repository.SkillRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 클라이언트에 내려줄 기획 데이터 조회
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameDataService {

    private final GameDataVersionRepository gameDataVersionRepository;
    private final SkillRepository skillRepository;

    public DataVersionsResponse getVersions() {
        return new DataVersionsResponse(currentVersion(SkillDataService.DATA_TYPE));
    }

    // 버전과 목록을 한 트랜잭션에서 읽어 응답의 version과 datas가 항상 같은 반영분이 되게 한다
    public SkillsResponse getSkills() {
        int version = currentVersion(SkillDataService.DATA_TYPE);
        List<SkillResponse> datas = skillRepository.findAll(Sort.by("skillId")).stream()
                .map(SkillResponse::from)
                .toList();
        return new SkillsResponse(version, datas);
    }

    // 아직 반영된 적이 없으면 0
    private int currentVersion(String dataType) {
        return gameDataVersionRepository.findById(dataType)
                .map(GameDataVersion::getVersion)
                .orElse(0);
    }
}
