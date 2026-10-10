package com.couserver.player.service;

import com.couserver.inventory.entity.ItemGrade;
import com.couserver.staticdata.service.StaticDataService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import java.util.ArrayList;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

// 장비 1개의 스탯에 레벨/등급 배율을 적용
// 스탯 = round(값 x (1 + statGrowPerLevel x (level - 1)) x gradeStatMultiplier[grade])
@Component
public class EquipmentStatCalculator {
    private static final String TABLE_NAME = "ItemConst";

    private final BigDecimal statGrowthPerLevel;
    private final List<BigDecimal> gradeStatMultiplier;

    public EquipmentStatCalculator(StaticDataService staticDataService) {
        JsonNode table = staticDataService.getTables().get(TABLE_NAME);
        require(table != null, "ItemConst 데이터가 없습니다.");
        JsonNode rows = table.get("datas");
        require(rows != null && rows.size() == 1, "ItemConst 데이터는 행이 하나여야 합니다.");
        JsonNode row = rows.get(0);

        JsonNode growth = row.get("statGrowthPerLevel");
        require(growth != null && growth.isNumber() && growth.decimalValue().signum() >= 0,
                "ItemConst의 statGrowthPerLevel은 0 이상이어야 합니다.");

        JsonNode multipliers = row.get("gradeStatMultiplier");
        require(multipliers != null && multipliers.isArray() && multipliers.size() == ItemGrade.values().length,
                "ItemConst의 gradeStatMultiplier는 등급 수(" + ItemGrade.values().length + ")와 개수가 같아야 합니다.");
        List<BigDecimal> list = new ArrayList<>();
        for (int i = 0; i < multipliers.size(); i++) {
            JsonNode m = multipliers.get(i);
            require(m.isNumber() && m.decimalValue().signum() > 0,
                    "ItemConst의 gradeStatMultiplier 값은 0보다 커야 합니다.");
            list.add(m.decimalValue());
        }

        this.statGrowthPerLevel = growth.decimalValue();
        this.gradeStatMultiplier = List.copyOf(list);
    }

    public int scale(int value, int level, ItemGrade grade){
        if (level < 1)
            throw new IllegalArgumentException("장비 레벨은 1 이상이어야 합니다 : " + level);

        BigDecimal levelFactor = BigDecimal.ONE.add(
                statGrowthPerLevel.multiply(BigDecimal.valueOf(level - 1L))
        );
        BigDecimal result = BigDecimal.valueOf(value)
                .multiply(levelFactor)
                .multiply(gradeStatMultiplier.get(grade.ordinal()));

        return result.setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    private static void require(boolean condition, String message){
        if (!condition){
            throw new IllegalStateException(message);
        }
    }
}
