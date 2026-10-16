package com.couserver.player.service;

import com.couserver.common.exception.BusinessException;
import com.couserver.inventory.entity.Equipment;
import com.couserver.inventory.entity.ItemGrade;
import com.couserver.inventory.repository.EquipmentRepository;
import com.couserver.player.dto.PlayerFinalStatResponse;
import com.couserver.player.dto.PlayerFinalStatResponse.Breakdown;
import com.couserver.player.dto.PlayerFinalStatResponse.StatSource;
import com.couserver.player.entity.PlayerStat;
import com.couserver.player.entity.PlayerStatType;
import com.couserver.player.exception.PlayerErrorCode;
import com.couserver.player.repository.PlayerStatRepository;
import com.couserver.player.service.EquipmentEffectCalculator.EquipmentEffectSum;
import java.util.List;
import com.couserver.staticdata.dto.EvolutionData;
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
    private final PlayerStatRepository playerStatRepository;
    private final EquipmentRepository equipmentRepository;

    private final StaticDataService staticDataService;
    private final EquipmentStatCalculator equipmentStatCalculator;
    private final EquipmentEffectCalculator equipmentEffectCalculator;   // 장비 등급 효과 (HP%·ATTACK%·DEFENSE)


    // 최종 스탯 = (기초 + 장비 고정값 + 진화) × (100 + 보너스%) / 100, 정수 나눗셈(버림)
    // 보너스%는 장비 등급 효과의 HP%·ATTACK% 합이고, 방어력·포션 회복량은 0
    // 장비 방어력 고정값은 등급 효과의 DEFENSE 합이다 (ItemConst 배율 미적용)
    public PlayerFinalStatResponse getFinalStat(Long playerId){
        PlayerBaseStatData baseStat = staticDataService.getPlayerBaseStat();

        PlayerStat playerStat = playerStatRepository.findById(playerId)
                .orElseThrow(() -> new BusinessException(PlayerErrorCode.PLAYER_STAT_NOT_FOUND));

        List<Equipment> equipped = equipmentRepository.findByPlayerId(playerId).stream()
                .filter(Equipment::isEquipped)
                .toList();
        EquipmentEffectSum effects = equipmentEffectCalculator.sum(equipped);

        StatSource base = new StatSource(baseStat.playerBaseAttack(), baseStat.playerBaseHp(), 0, 0);
        StatSource equipment = sumEquipped(equipped)
                .plus(new StatSource(0, 0, effects.defense(), 0));
        StatSource evolution = evolutionOf(playerStat);
        StatSource bonusPercent = new StatSource(effects.attackPercent(), effects.hpPercent(), 0, 0);
        StatSource total = base.plus(equipment).plus(evolution);

        int finalAttack = applyBonus(total.attack(), bonusPercent.attack());
        int finalHp = applyBonus(total.hp(), bonusPercent.hp());
        int finalDefense = applyBonus(total.defense(), bonusPercent.defense());
        int finalPotionRecovery = applyBonus(total.potionRecovery(), bonusPercent.potionRecovery());

        // 전투력은 보너스% 적용 후의 최종값으로 계산한다 (클라이언트 장비창 표시와 같은 기준)
        int combatPower = staticDataService.getCombatPower().calculate(finalAttack, finalHp, finalDefense);

        return new PlayerFinalStatResponse(
                finalAttack,
                finalHp,
                finalDefense,
                finalPotionRecovery,
                combatPower,
                baseStat.playerBaseCriticalDamage(),
                baseStat.playerBaseCriticalChance(),
                baseStat.playerBaseSkillDamage(),
                baseStat.playerBaseMoveSpeed(),
                baseStat.playerBaseMaxMoveSpeed(),
                baseStat.playerBaseLootRadius(),
                new Breakdown(base, equipment, evolution, bonusPercent));
    }

    // 장착 중인 장비의 기본 보너스 합 (레벨·등급 배율 적용)
    private StatSource sumEquipped(List<Equipment> equipped){
        return equipped.stream()
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

    // 진화 레벨 × 레벨당 상승량 (Evolution 시트의 evolutionIncreasePerLevel)
    private StatSource evolutionOf(PlayerStat stat) {
        EvolutionData evolution = staticDataService.getEvolution();
        return new StatSource(
                stat.getPlayerStatAttackLevel() * evolution.increasePerLevel(PlayerStatType.ATTACK),
                stat.getPlayerStatHpLevel() * evolution.increasePerLevel(PlayerStatType.HP),
                stat.getPlayerStatDefenseLevel() * evolution.increasePerLevel(PlayerStatType.DEFENSE),
                stat.getPlayerStatPotionRecoveryLevel() * evolution.increasePerLevel(PlayerStatType.POTION_RECOVERY));
    }

    private int applyBonus(int value, int bonusPercent){
        return value * (100 + bonusPercent) / 100;
    }
}
