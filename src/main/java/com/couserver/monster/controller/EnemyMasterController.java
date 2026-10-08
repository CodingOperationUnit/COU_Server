package com.couserver.monster.controller;

import com.couserver.common.exception.BusinessException;
import com.couserver.master.exception.MasterErrorCode;
import com.couserver.monster.table.MonsterAttackTable;
import com.couserver.monster.table.MonsterTable;
import com.couserver.monster.table.SpawnPatternTable;
import com.couserver.monster.table.WaveTable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/master/enemy")
public class EnemyMasterController {

    private final MonsterTable monsterTable;
    private final MonsterAttackTable monsterAttackTable;
    private final SpawnPatternTable spawnPatternTable;
    private final WaveTable waveTable;

    @GetMapping("/{tableName}")
    public ResponseEntity<JsonNode> getTable(@PathVariable String tableName) {
        JsonNode body = switch (tableName) {
            case MonsterTable.TABLE_NAME -> monsterTable.original();
            case MonsterAttackTable.TABLE_NAME -> monsterAttackTable.original();
            case SpawnPatternTable.TABLE_NAME -> spawnPatternTable.original();
            case WaveTable.TABLE_NAME         -> waveTable.original();
            default -> throw new BusinessException(MasterErrorCode.MASTER_TABLE_NOT_FOUND);
        };
        return ResponseEntity.ok(body);
    }
}