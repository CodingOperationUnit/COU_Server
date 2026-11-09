package com.couserver.player.service;

import com.couserver.inventory.entity.ItemGrade;
import com.couserver.staticdata.dto.GradeData;
import com.couserver.staticdata.service.StaticDataService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import java.util.ArrayList;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

// 장비 1개의 스탯에 레벨·등급 배율을 적용한다 (클라이언트 OwnedItem과 같은 공식)
// 스탯 = round(값 × (1 + statGrowthPerLevel × (level − 1)) × gradeStatMultiplier[grade])
// 부동소수 오차가 없도록 BigDecimal로 계산하고, 0.5는 짝수 쪽으로 반올림(HALF_EVEN)한다
// (클라이언트 Mathf.RoundToInt·decimal Math.Round와 같은 규칙: 38.5 → 38, 39.5 → 40)
@Component
public class EquipmentStatCalculator {
    private static final String TABLE_NAME = "ItemConst";

    private final BigDecimal statGrowthPerLevel;
    private final Map<ItemGrade, GradeData> grades;

    public EquipmentStatCalculator(StaticDataService staticDataService) {
        JsonNode table = staticDataService.getTables().get(TABLE_NAME);
        require(table != null, "ItemConst 데이터가 없습니다.");
        JsonNode rows = table.get("datas");
        require(rows != null && rows.size() == 1, "ItemConst 데이터는 행이 하나여야 합니다.");
        JsonNode row = rows.get(0);

        JsonNode growth = row.get("statGrowthPerLevel");
        require(growth != null && growth.isNumber() && growth.decimalValue().signum() >= 0,
                "ItemConst의 statGrowthPerLevel은 0 이상이어야 합니다.");

        this.statGrowthPerLevel = growth.decimalValue();
        this.grades = staticDataService.getGrades();

        require(
                grades != null && grades.size() == ItemGrade.values().length,
                "Grade 데이터가 장비 등급 수("
                        + ItemGrade.values().length
                        + ")와 일치하지 않습니다."
        );
    }

    public int scale(int value, int level, ItemGrade grade){
        if (level < 1)
            throw new IllegalArgumentException("장비 레벨은 1 이상이어야 합니다 : " + level);

        BigDecimal levelFactor = BigDecimal.ONE.add(
                statGrowthPerLevel.multiply(BigDecimal.valueOf(level - 1L))
        );
        GradeData gradeData = grades.get(grade);

        require(
                gradeData != null,
                "Grade에 없는 장비 등급입니다: " + grade
        );

        BigDecimal result = BigDecimal.valueOf(value)
                .multiply(levelFactor)
                .multiply(gradeData.statMultiplier());

        return result.setScale(0, RoundingMode.HALF_EVEN).intValueExact();
    }

    private static void require(boolean condition, String message){
        if (!condition){
            throw new IllegalStateException(message);
        }
    }
}
