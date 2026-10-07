package com.couserver.monster.controller;

import com.couserver.monster.dto.MonsterResponse;
import com.couserver.monster.service.MonsterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MonsterController {

    private final MonsterService monsterService;

    @GetMapping("/monsters")
    public ResponseEntity<List<MonsterResponse>> getAllMonsters() {
        return ResponseEntity.ok(monsterService.findAll());
    }
}
