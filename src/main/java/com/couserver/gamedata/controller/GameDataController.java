package com.couserver.gamedata.controller;

import com.couserver.gamedata.dto.DataVersionsResponse;
import com.couserver.gamedata.dto.SkillsResponse;
import com.couserver.gamedata.service.GameDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 로그인 전에 호출하므로 인증 없이 열려 있다 (SecurityConfig permitAll)
@RestController
@RequestMapping("/api/v1/data")
@RequiredArgsConstructor
public class GameDataController {

    private final GameDataService gameDataService;

    @GetMapping("/versions")
    public DataVersionsResponse getVersions() {
        return gameDataService.getVersions();
    }

    @GetMapping("/skills")
    public SkillsResponse getSkills() {
        return gameDataService.getSkills();
    }
}
