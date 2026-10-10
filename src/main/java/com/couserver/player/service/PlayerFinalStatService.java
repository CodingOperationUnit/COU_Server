package com.couserver.player.service;

import com.couserver.common.exception.BusinessException;
import com.couserver.inventory.entity.Equipment;
import com.couserver.inventory.entity.ItemGrade;
import com.couserver.inventory.repository.EquipmentRepository;
import com.couserver.player.dto.PlayerFinalStatResponse;
import com.couserver.player.dto.PlayerFinalStatResponse.Breakdown;
import com.couserver.player.dto.PlayerFinalStatResponse.StatSource;
import com.couserver.player.dummy.EvolutionDummyData;
import com.couserver.player.entity.PlayerStat;
import com.couserver.player.entity.PlayerStatType;
import com.couserver.player.exception.PlayerErrorCode;
import com.couserver.player.repository.PlayerStatRepository;
import com.couserver.staticdata.dto.ItemData;
import com.couserver.staticdata.dto.PlayerBaseStatData;
import com.couserver.staticdata.service.StaticDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 최종 스탯 = (기초 + 장착 장비 + 진화) x (100 + 보너스%) / 100
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerFinalStatService {
    // 장비 보너스
    // TODO: 등급 스킬 규칙이 정해지면 장비에서 계산
    private static final int BONUS_PERCENT = 0;

    private final PlayerStatRepository playerStatRepository;
    private final EquipmentRepository equipmentRepository;

    private final StaticDataService staticDataService;
    private final EquipmentStatCalculator equipmentStatCalculator;

    public PlayerFinalStatResponse getFinalStat(Long playerId){
        PlayerBaseStatData baseStat = staticDataService.getPlayerBaseStat();

        PlayerStat playerStat = playerStatRepository.findById(playerId)
                .orElseThrow(() -> new BusinessException(PlayerErrorCode.PLAYER_STAT_NOT_FOUND));

        StatSource base = new StatSource(baseStat.playerBaseAttack(), baseStat.playerBaseHp(), 0, 0);
        StatSource equipment = sumEquipped(playerId);
        StatSource evolution = evolutionOf(playerStat);
        StatSource total = base.plus(equipment).plus(evolution);

        return new PlayerFinalStatResponse(
                applyBonus(total.attack()),
                applyBonus(total.hp()),
                applyBonus(total.defense()),
                applyBonus(total.potionRecovery()),
                baseStat.playerBaseCriticalDamage(),
                baseStat.playerBaseCriticalChance(),
                baseStat.playerBaseSkillDamage(),
                baseStat.playerBaseMoveSpeed(),
                baseStat.playerBaseMaxMoveSpeed(),
                baseStat.playerBaseLootRadius(),
                new Breakdown(base, equipment, evolution));
    }

    // 장착중인 장비의 스탯 합
    private StatSource sumEquipped(Long playerId){
        return equipmentRepository.findByPlayerId(playerId).stream()
                .filter(Equipment::isEquipped)
                .map(this::statOf)
                .reduce(StatSource.ZERO, StatSource::plus);
    }

    // 장비 1개가 주는 스탯 - 장비 스탯을 꺼내는 곳은 여기 한 곳 뿐
    private StatSource statOf(Equipment equipment){
        ItemData item = staticDataService.getItems().get(equipment.getItemId());   // 정적 데이터에서 아이템 정보 조회
        if (item == null)
            throw new IllegalStateException("정적 데이터에 없는 아이템입니다: " + equipment.getItemId());
        int level = equipment.getLevel();
        ItemGrade grade = equipment.getGrade();
        return new StatSource(
                equipmentStatCalculator.scale(item.attackBonus(), level, grade),
                equipmentStatCalculator.scale(item.hpBonus(), level, grade),
                0,
                0
        );
    }

    // 진화레벨 x 레벨당 상승량 (더미데이터)
    private StatSource evolutionOf(PlayerStat stat){
        return new StatSource(
                stat.getPlayerStatAttackLevel() * EvolutionDummyData.getIncreasePerLevel(PlayerStatType.ATTACK),
                stat.getPlayerStatHpLevel() * EvolutionDummyData.getIncreasePerLevel(PlayerStatType.HP),
                stat.getPlayerStatDefenseLevel() * EvolutionDummyData.getIncreasePerLevel(PlayerStatType.DEFENSE),
                stat.getPlayerStatPotionRecoveryLevel() * EvolutionDummyData.getIncreasePerLevel(PlayerStatType.POTION_RECOVERY)
        );
    }

    // 정수 나눗셈으로 버림
    private int applyBonus(int value){
        return value * (100 + BONUS_PERCENT) / 100;
    }
}
