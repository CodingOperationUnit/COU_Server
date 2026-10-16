package com.couserver.player.service;

import com.couserver.inventory.entity.Equipment;
import com.couserver.inventory.entity.ItemGrade;
import com.couserver.staticdata.dto.EquipEffectData;
import com.couserver.staticdata.service.StaticDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

// 장착 장비의 등급 효과 합산 (명세 4장 스탯 계산 1~3)
// - 장비 인스턴스 등급 이하에 연결된 효과를 모두 적용(누적)하고, 같은 스탯은 합산한다
// - 제외(79xxx)와 Stat형이 아닌 효과는 건너뛴다
// - 서버는 HP%·ATTACK%·DEFENSE(고정값)만 반영한다. 나머지 스탯은 클라이언트가 전투에서 쓴다
// - ItemConst의 레벨·등급 배율은 등급 효과에 적용하지 않는다
@Component
public class EquipmentEffectCalculator {

    // 등급 효과 합. hpPercent·attackPercent는 보너스%, defense는 더하는 고정값
    public record EquipmentEffectSum(int hpPercent, int attackPercent, int defense) {
        public static final EquipmentEffectSum ZERO = new EquipmentEffectSum(0, 0, 0);

        public EquipmentEffectSum plus(EquipmentEffectSum other) {
            return new EquipmentEffectSum(
                    hpPercent + other.hpPercent,
                    attackPercent + other.attackPercent,
                    defense + other.defense);
        }
    }

    private final Map<Long, Map<ItemGrade, List<EquipEffectData>>> itemGradeEffects;   // itemId → 해금 등급 → 효과

    @Autowired
    public EquipmentEffectCalculator(StaticDataService staticDataService) {
        this(staticDataService.getItemGradeEffects());
    }

    // 테스트에서 데이터를 직접 넣을 때 쓴다
    EquipmentEffectCalculator(Map<Long, Map<ItemGrade, List<EquipEffectData>>> itemGradeEffects) {
        this.itemGradeEffects = itemGradeEffects;
    }

    // 장착 장비 여러 개의 합
    public EquipmentEffectSum sum(Collection<Equipment> equippedItems) {
        return equippedItems.stream()
                .map(e -> effectsOf(e.getItemId(), e.getGrade()))
                .reduce(EquipmentEffectSum.ZERO, EquipmentEffectSum::plus);
    }

    // 장비 1개: 이 아이템의 효과 중 인스턴스 등급 이하에서 해금된 것만 합산한다
    public EquipmentEffectSum effectsOf(long itemId, ItemGrade instanceGrade) {
        int hpPercent = 0;
        int attackPercent = 0;
        int defense = 0;

        for (Map.Entry<ItemGrade, List<EquipEffectData>> entry
                : itemGradeEffects.getOrDefault(itemId, Map.of()).entrySet()) {
            if (entry.getKey().ordinal() > instanceGrade.ordinal()) {
                continue;   // 아직 잠긴 등급
            }
            for (EquipEffectData effect : entry.getValue()) {
                if (!effect.isActiveStat()) {
                    continue;   // 제외 대상이거나 Stat형이 아님
                }
                // HP·ATTACK·DEFENSE 값은 시작 시 검사에서 정수임을 확인했다
                int value = (int) effect.effectValue();
                switch (effect.statType()) {
                    case HP -> hpPercent += value;
                    case ATTACK -> attackPercent += value;
                    case DEFENSE -> defense += value;
                    default -> {
                        // 이동속도·치명타·회피·피해 감소·치유량은 서버 최종 스탯에 넣지 않는다
                    }
                }
            }
        }
        return new EquipmentEffectSum(hpPercent, attackPercent, defense);
    }
}