package com.couserver.staticdata;

import com.couserver.inventory.entity.ItemGrade;
import com.couserver.staticdata.dto.DropRateData;
import com.couserver.staticdata.dto.GradeData;
import com.couserver.staticdata.service.StaticDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GradeDropRateValidationTest {

    private static GradeData grade(int order, ItemGrade grade, String multiplier, boolean canDrop) {
        return new GradeData(order, grade, new BigDecimal(multiplier), canDrop);
    }

    // 시트와 같은 구성
    private static List<GradeData> validGrades() {
        return List.of(
                grade(1, ItemGrade.COMMON, "1.0", true),
                grade(2, ItemGrade.UNCOMMON, "1.12", true),
                grade(3, ItemGrade.RARE, "1.25", true),
                grade(4, ItemGrade.EPIC, "1.45", true),
                grade(5, ItemGrade.LEGENDARY, "1.7", false));
    }

    private static DropRateData rate(String id, int min, int max, String c, String u, String r, String e, String l) {
        return new DropRateData(id, min, max,
                new BigDecimal(c), new BigDecimal(u), new BigDecimal(r), new BigDecimal(e), new BigDecimal(l));
    }

    // 시트와 같은 구성 (L02는 소수점 셋째 자리까지 있는 구간)
    private static List<DropRateData> validRates() {
        return List.of(
                rate("L01", 1, 9, "0.7", "0.24", "0.05", "0.01", "0"),
                rate("L02", 10, 19, "0.65", "0.27", "0.065", "0.015", "0"),
                rate("L03", 20, 29, "0.6", "0.29", "0.09", "0.02", "0"),
                rate("L04", 30, 39, "0.55", "0.31", "0.11", "0.03", "0"),
                rate("L05", 40, 0, "0.5", "0.32", "0.14", "0.04", "0"));
    }

    private static Map<ItemGrade, GradeData> grades() {
        return StaticDataService.validateGrades(validGrades());
    }

    @Test
    @DisplayName("Grade: 시트 구성은 통과하고, 등급별로 찾을 수 있다")
    void validGradesPass() {
        Map<ItemGrade, GradeData> map = grades();
        assertThat(map).hasSize(5);
        assertThat(map.get(ItemGrade.RARE).statMultiplier()).isEqualByComparingTo("1.25");
        assertThat(map.get(ItemGrade.LEGENDARY).canDrop()).isFalse();
    }

    @Test
    @DisplayName("Grade: 행 수가 등급 수와 다르면 실패한다")
    void gradeCountMismatch() {
        assertThatThrownBy(() -> StaticDataService.validateGrades(validGrades().subList(0, 3)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("등급 수");
    }

    @Test
    @DisplayName("Grade: 순서의 등급 코드가 ItemGrade와 다르면 실패한다")
    void gradeOrderMismatch() {
        List<GradeData> swapped = List.of(
                grade(1, ItemGrade.UNCOMMON, "1.0", true),
                grade(2, ItemGrade.COMMON, "1.12", true),
                grade(3, ItemGrade.RARE, "1.25", true),
                grade(4, ItemGrade.EPIC, "1.45", true),
                grade(5, ItemGrade.LEGENDARY, "1.7", false));
        assertThatThrownBy(() -> StaticDataService.validateGrades(swapped))
                .hasMessageContaining("grade는 Common");
    }

    @Test
    @DisplayName("Grade: 배율이 0이면 실패한다")
    void gradeZeroMultiplier() {
        List<GradeData> zero = List.of(
                grade(1, ItemGrade.COMMON, "0", true),
                grade(2, ItemGrade.UNCOMMON, "1.12", true),
                grade(3, ItemGrade.RARE, "1.25", true),
                grade(4, ItemGrade.EPIC, "1.45", true),
                grade(5, ItemGrade.LEGENDARY, "1.7", false));
        assertThatThrownBy(() -> StaticDataService.validateGrades(zero))
                .hasMessageContaining("statMultiplier");
    }

    @Test
    @DisplayName("DropRate: 시트 구성은 통과한다 (0.065처럼 셋째 자리 소수도 합계가 정확히 1)")
    void validRatesPass() {
        assertThatCode(() -> StaticDataService.validateDropRates(validRates(), grades())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("DropRate: 확률 합계가 1이 아니면 실패한다")
    void rateSumNotOne() {
        List<DropRateData> rows = List.of(
                rate("L01", 1, 0, "0.7", "0.24", "0.05", "0.02", "0"));
        assertThatThrownBy(() -> StaticDataService.validateDropRates(rows, grades()))
                .hasMessageContaining("합계");
    }

    @Test
    @DisplayName("DropRate: 일반 획득 불가 등급(Legendary)의 확률이 0보다 크면 실패한다")
    void legendaryRateMustBeZero() {
        List<DropRateData> rows = List.of(
                rate("L01", 1, 0, "0.7", "0.24", "0.05", "0", "0.01"));
        assertThatThrownBy(() -> StaticDataService.validateDropRates(rows, grades()))
                .hasMessageContaining("일반 획득 불가");
    }

    @Test
    @DisplayName("DropRate: 구간이 겹치거나 비면 실패한다")
    void rangesMustBeContiguous() {
        List<DropRateData> overlap = List.of(
                rate("L01", 1, 10, "1", "0", "0", "0", "0"),
                rate("L02", 10, 0, "1", "0", "0", "0", "0"));
        List<DropRateData> gap = List.of(
                rate("L01", 1, 9, "1", "0", "0", "0", "0"),
                rate("L02", 11, 0, "1", "0", "0", "0", "0"));
        assertThatThrownBy(() -> StaticDataService.validateDropRates(overlap, grades())).hasMessageContaining("겹침");
        assertThatThrownBy(() -> StaticDataService.validateDropRates(gap, grades())).hasMessageContaining("겹침");
    }

    @Test
    @DisplayName("DropRate: 상한 없음(0)은 마지막 구간에만, 마지막 구간은 반드시 상한 없음")
    void unlimitedOnlyLast() {
        List<DropRateData> middleUnlimited = List.of(
                rate("L01", 1, 0, "1", "0", "0", "0", "0"),
                rate("L02", 10, 0, "1", "0", "0", "0", "0"));
        List<DropRateData> lastLimited = List.of(
                rate("L01", 1, 9, "1", "0", "0", "0", "0"),
                rate("L02", 10, 19, "1", "0", "0", "0", "0"));
        assertThatThrownBy(() -> StaticDataService.validateDropRates(middleUnlimited, grades()))
                .hasMessageContaining("마지막 구간이 아니라");
        assertThatThrownBy(() -> StaticDataService.validateDropRates(lastLimited, grades()))
                .hasMessageContaining("상한 없음");
    }

    @Test
    @DisplayName("DropRate: 첫 구간이 1레벨부터 시작하지 않으면 실패한다")
    void firstRangeStartsAtOne() {
        List<DropRateData> rows = List.of(rate("L01", 2, 0, "1", "0", "0", "0", "0"));
        assertThatThrownBy(() -> StaticDataService.validateDropRates(rows, grades()))
                .hasMessageContaining("minAccountLevel은 1");
    }

    @Test
    @DisplayName("구간 조회: 레벨마다 맞는 구간을 찾는다")
    void covers() {
        List<DropRateData> rows = StaticDataService.validateDropRates(validRates(), grades());
        assertThat(find(rows, 1)).isEqualTo("L01");
        assertThat(find(rows, 9)).isEqualTo("L01");
        assertThat(find(rows, 10)).isEqualTo("L02");
        assertThat(find(rows, 40)).isEqualTo("L05");
        assertThat(find(rows, 100)).isEqualTo("L05");
    }

    private static String find(List<DropRateData> rows, int level) {
        return rows.stream().filter(r -> r.covers(level)).findFirst().orElseThrow().dropRateId();
    }

    @Test
    @DisplayName("계정 레벨 1의 전체 등급 추첨에서 Legendary가 나오지 않는다")
    void rollGradeNeverDropsLegendary() {
        DropRateData rate = validRates().get(0);

        assertThat(StaticDataService.selectEquipmentGrade(
                rate, grades(),
                ItemGrade.COMMON, ItemGrade.LEGENDARY,
                0.995
        )).isEqualTo(ItemGrade.EPIC);
    }

    @Test
    @DisplayName("허용 등급 범위 안에서 확률을 재정규화한다")
    void rollGradeWithinRange() {
        DropRateData rate = validRates().get(0);

        assertThat(StaticDataService.selectEquipmentGrade(
                rate, grades(),
                ItemGrade.UNCOMMON, ItemGrade.RARE,
                0.80
        )).isEqualTo(ItemGrade.UNCOMMON);

        assertThat(StaticDataService.selectEquipmentGrade(
                rate, grades(),
                ItemGrade.UNCOMMON, ItemGrade.RARE,
                0.90
        )).isEqualTo(ItemGrade.RARE);
    }

    @Test
    @DisplayName("획득 불가능한 등급만 허용하면 예외가 발생한다")
    void rollGradeRejectsImpossibleRange() {
        DropRateData rate = validRates().get(0);

        assertThatThrownBy(() -> StaticDataService.selectEquipmentGrade(
                rate, grades(),
                ItemGrade.LEGENDARY, ItemGrade.LEGENDARY,
                0.5
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("획득 가능한 장비가 없습니다");
    }
}