package com.couserver.monster.controller;

import com.couserver.monster.service.MonsterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MonsterController {

    private final MonsterService monsterService;
}
