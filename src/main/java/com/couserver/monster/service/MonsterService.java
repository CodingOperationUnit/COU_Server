package com.couserver.monster.service;

import com.couserver.monster.dto.MonsterResponse;
import com.couserver.monster.entity.Monster;
import com.couserver.monster.repository.MonsterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonsterService {

    private final MonsterRepository monsterRepository;

    public List<MonsterResponse> findAll() {
        return monsterRepository.findAll().stream()
                .map(MonsterResponse::from)
                .toList();
    }
}
