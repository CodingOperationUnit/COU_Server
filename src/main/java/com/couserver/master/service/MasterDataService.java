package com.couserver.master.service;

import com.couserver.common.exception.BusinessException;
import com.couserver.master.dto.MasterTableResponse;
import com.couserver.master.dto.MasterVersionsResponse;
import com.couserver.master.dto.PlayerBaseStatData;
import com.couserver.master.entity.MasterTableVersion;
import com.couserver.master.entity.PlayerBaseStat;
import com.couserver.master.exception.MasterErrorCode;
import com.couserver.master.repository.MasterTableVersionRepository;
import com.couserver.master.repository.PlayerBaseStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MasterDataService {
    private final MasterTableVersionRepository masterTableVersionRepository;
    private final PlayerBaseStatRepository playerBaseStatRepository;

    // 반영된 모든 테이블 버전 (이름순)
    public MasterVersionsResponse getVersions(){
        List<MasterVersionsResponse.TableVersion> tables = masterTableVersionRepository
                .findAll(Sort.by("tableName"))
                .stream()
                .map(MasterVersionsResponse.TableVersion::from)
                .toList();

        return new MasterVersionsResponse(tables);
    }

    // 테이블 하나의 데이터와 버전
    // TODO : 데이블이 늘어나면 방법 B (테이블 별 Provider 등록 구조) 검토 - 리펙토링 단계에서
    public MasterTableResponse getTable(String tableName){
        return switch (tableName){
            case PlayerBaseStatDataLoader.TABLE_NAME -> getPlayerBaseStat();
            default -> throw new BusinessException(MasterErrorCode.MASTER_TABLE_NOT_FOUND);
        };
    }

    private MasterTableResponse getPlayerBaseStat() {
        MasterTableVersion version = masterTableVersionRepository
                .findById(PlayerBaseStatDataLoader.TABLE_NAME)
                .orElseThrow(() -> new IllegalStateException("player-base-stat 버전 정보가 없습니다."));

        PlayerBaseStat stat = playerBaseStatRepository
                .findById(PlayerBaseStat.SINGLE_ID)
                .orElseThrow(() -> new IllegalStateException("player-base-stat 데이터가 없습니다."));

        PlayerBaseStatData data = new PlayerBaseStatData(
                stat.getPlayerBaseAttack(), stat.getPlayerBaseHp(), stat.getPlayerBaseCriticalDamage(),
                stat.getPlayerBaseCriticalChance(), stat.getPlayerBaseSkillDamage(), stat.getPlayerBaseMoveSpeed(),
                stat.getPlayerBaseMaxMoveSpeed(), stat.getPlayerBaseLootRadius()
        );

        return MasterTableResponse.ofObject(PlayerBaseStatDataLoader.TABLE_NAME, version.getVersion(), data);
    }
}
