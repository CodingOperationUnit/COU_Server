package com.couserver.player;

import com.couserver.inventory.entity.Equipment;
import com.couserver.inventory.entity.ItemGrade;
import com.couserver.inventory.repository.EquipmentRepository;
import com.couserver.staticdata.service.StaticDataService;
import com.couserver.support.IntegrationTestSupport;
import com.couserver.support.TestAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 장비 등급 효과가 최종 스탯(E2)과 전투력 랭킹에 반영되는지 확인한다
// 기준 아이템: 1003 퀀텀 실드 아머 (General 체력 +10%, Super 체력 +15%, Rare 방어력 +12·받는 피해 감소 8%)
class EquipmentGradeEffectApiTest extends IntegrationTestSupport {
    private static final String STATS_URL = "/api/players/me/stats";
    private static final String INVENTORY_URL = "/api/inventory";
    private static final String COMBAT_POWER_RANKING_URL = "/api/ranking?type=COMBAT_POWER";
    private static final long QUANTUM_SHIELD_ARMOR = 1003;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Test
    @DisplayName("1003을 Rare·레벨 1로 장착하면 최종 체력은 1438이다 (명세 예시)")
    void rareExample() throws Exception {
        TestAccount account = createAccount();
        long id = giveEquipment(account, ItemGrade.RARE);
        equip(account, id);

        getAs(account, STATS_URL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalHp").value(1438))                       // (1000 + 151) × 125 / 100
                .andExpect(jsonPath("$.finalAttack").value(114))                    // (100 + 14) × 100 / 100
                .andExpect(jsonPath("$.finalDefense").value(12))                    // 진화 0 + 방어력 효과 12
                .andExpect(jsonPath("$.breakdown.equipment.hp").value(151))         // 55 × 2.75, 배율은 고정값에만
                .andExpect(jsonPath("$.breakdown.equipment.defense").value(12))
                .andExpect(jsonPath("$.breakdown.bonusPercent.hp").value(25))       // General 10 + Super 15 누적
                .andExpect(jsonPath("$.breakdown.bonusPercent.attack").value(0));
    }

    @Test
    @DisplayName("인스턴스 등급 이하의 효과만 누적된다")
    void accumulatesUpToInstanceGrade() throws Exception {
        TestAccount account = createAccount();
        long id = giveEquipment(account, ItemGrade.GENERAL);
        equip(account, id);

        getAs(account, STATS_URL)
                .andExpect(jsonPath("$.breakdown.bonusPercent.hp").value(10))
                .andExpect(jsonPath("$.breakdown.equipment.defense").value(0))
                .andExpect(jsonPath("$.finalHp").value(1160));                      // (1000 + 55) × 110 / 100

        setGrade(id, ItemGrade.SUPER);
        getAs(account, STATS_URL)
                .andExpect(jsonPath("$.breakdown.bonusPercent.hp").value(25))
                .andExpect(jsonPath("$.breakdown.equipment.defense").value(0))
                .andExpect(jsonPath("$.finalHp").value(1370));                      // (1000 + 96) × 125 / 100
    }

    @Test
    @DisplayName("장착하지 않은 장비의 등급 효과는 반영되지 않는다")
    void unequippedIgnored() throws Exception {
        TestAccount account = createAccount();
        giveEquipment(account, ItemGrade.RARE);

        getAs(account, STATS_URL)
                .andExpect(jsonPath("$.finalHp").value(1000))
                .andExpect(jsonPath("$.finalDefense").value(0))
                .andExpect(jsonPath("$.breakdown.bonusPercent.hp").value(0));
    }

    @Test
    @DisplayName("장착 장비를 합성해 등급이 오르면 전투력 랭킹도 등급 효과 반영 후 값으로 바뀐다")
    void rankingUpdatedAfterSynthesis() throws Exception {
        TestAccount account = createAccount();
        long target = giveEquipment(account, ItemGrade.GENERAL);
        giveEquipment(account, ItemGrade.GENERAL);
        giveEquipment(account, ItemGrade.GENERAL);
        equip(account, target);

        int before = combatPower(account);
        assertThat(rankingValue(account)).isEqualTo(before);

        postAs(account, INVENTORY_URL + "/" + target + "/synthesize")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventoryItemGrade").value(ItemGrade.SUPER.toClientName()));

        int after = combatPower(account);
        assertThat(after).isGreaterThan(before);                // 체력 +10% → +25%, 장비 고정값도 Super 배율
        assertThat(rankingValue(account)).isEqualTo(after);     // 랭킹이 E2와 같은 값
    }

    // 테스트 준비: 1003을 원하는 등급으로 지급한다 (Equipment.create는 아이템 원래 등급 Rare로 만든다)
    private long giveEquipment(TestAccount account, ItemGrade grade) {
        long id = equipmentRepository.save(
                Equipment.create(account.playerId(), staticDataService.getItems().get(QUANTUM_SHIELD_ARMOR))).getId();
        setGrade(id, grade);
        return id;
    }

    private void setGrade(long equipmentId, ItemGrade grade) {
        int updated = jdbcTemplate.update("UPDATE equipment SET grade = ? WHERE id = ?", grade.name(), equipmentId);
        assertThat(updated).as("equipment 행이 하나 갱신되어야 한다").isEqualTo(1);
    }

    private void equip(TestAccount account, long equipmentId) throws Exception {
        postAs(account, INVENTORY_URL + "/" + equipmentId + "/equip").andExpect(status().isOk());
    }

    private int combatPower(TestAccount account) throws Exception {
        return objectMapper.readTree(bodyOf(getAs(account, STATS_URL).andExpect(status().isOk())))
                .get("combatPower").asInt();
    }

    private int rankingValue(TestAccount account) throws Exception {
        return objectMapper.readTree(bodyOf(getAs(account, COMBAT_POWER_RANKING_URL).andExpect(status().isOk())))
                .get("myValue").asInt();
    }
}