package com.couserver.ledger;

import com.couserver.inventory.entity.Equipment;
import com.couserver.inventory.entity.ItemGrade;
import com.couserver.inventory.repository.EquipmentRepository;
import com.couserver.staticdata.dto.AchievementData;
import com.couserver.staticdata.dto.EvolutionData;
import com.couserver.staticdata.dto.ItemData;
import com.couserver.staticdata.dto.ShopProductData;
import com.couserver.staticdata.service.StaticDataService;
import com.couserver.support.IntegrationTestSupport;
import com.couserver.support.TestAccount;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 4-a-2: 재화가 바뀌는 모든 기능이 원장에 기록되고, 원장과 잔액이 어긋나지 않는다
class CurrencyLedgerFlowTest extends IntegrationTestSupport {
    private static final String PURCHASE_URL = "/api/shop/purchase";
    private static final String UPGRADE_URL = "/api/players/me/evolution/upgrade";
    private static final String INVENTORY_URL = "/api/inventory";
    private static final String ACHIEVEMENTS_URL = "/api/players/me/achievements";
    private static final int GOLD_PACK = 50011;
    private static final int RANDOM_ITEM = 50001;
    private static final int SKILL_KILL_ACHIEVEMENT_ID = 61001;
    private static final int LEVEL_UP_BASE_COST = 1000;   // InventoryService.LEVEL_UP_BASE_COST와 같은 값

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Test
    @DisplayName("전투 결과 골드가 전투 ID와 함께 기록된다")
    void battleResultIsRecorded() throws Exception {
        TestAccount account = createAccount();
        setGoldAndGem(account, 50, 0);

        long battleId = battleWithGold(account, 100);

        assertThat(entries(account)).singleElement().satisfies(row ->
                assertRow(row, "GOLD", 100, 150, "BATTLE_RESULT", battleId));
    }

    @Test
    @DisplayName("골드가 0인 전투 결과는 기록하지 않는다")
    void battleWithoutGoldIsNotRecorded() throws Exception {
        TestAccount account = createAccount();

        clearStage(account, staticDataService.getFirstStageId(), 0, 3, Map.of());

        assertThat(entries(account)).isEmpty();
    }

    @Test
    @DisplayName("골드 팩 구매는 보석 차감과 골드 지급 두 줄로 기록된다")
    void goldPackPurchaseIsRecorded() throws Exception {
        TestAccount account = createAccount();
        ShopProductData product = staticDataService.getShopProducts().get(GOLD_PACK);
        setGoldAndGem(account, 0, product.priceGem());

        postAs(account, PURCHASE_URL, Map.of("productId", GOLD_PACK)).andExpect(status().isOk());

        List<Map<String, Object>> rows = entries(account);
        assertThat(rows).hasSize(2);
        assertRow(rows.get(0), "GEM", -product.priceGem(), 0, "SHOP_PURCHASE", GOLD_PACK);
        assertRow(rows.get(1), "GOLD", product.rewardAmount(), product.rewardAmount(), "SHOP_PURCHASE", GOLD_PACK);
    }

    @Test
    @DisplayName("랜덤 장비 구매는 보석 차감만 기록된다")
    void randomItemPurchaseRecordsGemOnly() throws Exception {
        TestAccount account = createAccount();
        ShopProductData product = staticDataService.getShopProducts().get(RANDOM_ITEM);
        setGoldAndGem(account, 0, product.priceGem());

        postAs(account, PURCHASE_URL, Map.of("productId", RANDOM_ITEM)).andExpect(status().isOk());

        assertThat(entries(account)).singleElement().satisfies(row ->
                assertRow(row, "GEM", -product.priceGem(), 0, "SHOP_PURCHASE", RANDOM_ITEM));
    }

    @Test
    @DisplayName("진화 비용이 진화 후 단계와 함께 기록된다")
    void evolutionIsRecorded() throws Exception {
        TestAccount account = createAccount();
        EvolutionData evolution = staticDataService.getEvolution();
        int cost = evolution.costAt(0);
        setGoldAndGem(account, cost + 5, 0);

        postAs(account, UPGRADE_URL).andExpect(status().isOk());

        assertThat(entries(account)).singleElement().satisfies(row ->
                assertRow(row, "GOLD", -cost, 5, "EVOLUTION", 1L));
    }

    @Test
    @DisplayName("장비 레벨업 비용이 장비 ID와 함께 기록된다")
    void levelUpIsRecorded() throws Exception {
        TestAccount account = createAccount();
        long inventoryId = giveEquipment(account);
        setGoldAndGem(account, LEVEL_UP_BASE_COST + 500, 0);

        postAs(account, levelUpUrl(inventoryId)).andExpect(status().isOk());

        assertThat(entries(account)).singleElement().satisfies(row ->
                assertRow(row, "GOLD", -LEVEL_UP_BASE_COST, 500, "INVENTORY_LEVEL_UP", inventoryId));
    }

    @Test
    @DisplayName("일괄 레벨업은 차감 합계 한 줄로 기록된다")
    void levelUpBatchIsRecordedAsOneRow() throws Exception {
        TestAccount account = createAccount();
        long inventoryId = giveEquipment(account);
        int costToLevel3 = LEVEL_UP_BASE_COST * 1 + LEVEL_UP_BASE_COST * 2;   // 1→2, 2→3
        setGoldAndGem(account, costToLevel3 + 500, 0);

        postAs(account, levelUpUrl(inventoryId) + "/batch").andExpect(status().isOk());

        assertThat(entries(account)).singleElement().satisfies(row ->
                assertRow(row, "GOLD", -costToLevel3, 500, "INVENTORY_LEVEL_UP", inventoryId));
    }

    @Test
    @DisplayName("업적 보상이 업적 ID와 함께 기록된다")
    void achievementRewardIsRecorded() throws Exception {
        TestAccount account = createAccount();
        AchievementData achievement = staticDataService.getAchievements().get(SKILL_KILL_ACHIEVEMENT_ID);
        int firstTarget = achievement.targetValue().get(0);
        clearStage(account, staticDataService.getFirstStageId(), firstTarget, 3,
                Map.of(achievement.targetSkillId(), firstTarget));

        postAs(account, ACHIEVEMENTS_URL + "/" + SKILL_KILL_ACHIEVEMENT_ID + "/claim")
                .andExpect(status().isOk());

        int reward = achievement.rewardAmount().get(0);
        String type = achievement.rewardItemType().get(0).name();
        assertThat(entries(account)).singleElement().satisfies(row -> {
            assertThat(row.get("currency_type")).isEqualTo(type);
            assertThat(((Number) row.get("delta")).longValue()).isEqualTo(reward);
            assertThat(row.get("reason")).isEqualTo("ACHIEVEMENT_REWARD");
            assertThat(((Number) row.get("ref_id")).longValue()).isEqualTo(SKILL_KILL_ACHIEVEMENT_ID);
        });
    }

    @Test
    @DisplayName("재화 부족으로 실패한 요청은 기록을 남기지 않는다")
    void failedRequestsLeaveNoRecord() throws Exception {
        TestAccount account = createAccount();
        ShopProductData product = staticDataService.getShopProducts().get(GOLD_PACK);
        int evolutionCost = staticDataService.getEvolution().costAt(0);
        setGoldAndGem(account, evolutionCost - 1, product.priceGem() - 1);

        postAs(account, PURCHASE_URL, Map.of("productId", GOLD_PACK)).andExpect(status().isBadRequest());
        postAs(account, UPGRADE_URL).andExpect(status().isBadRequest());

        assertThat(entries(account)).isEmpty();
    }

    @Test
    @DisplayName("save로 재화를 부풀려도 원장에는 아무것도 남지 않는다")
    void saveDoesNotWriteLedger() throws Exception {
        TestAccount account = createAccount();
        setGoldAndGem(account, 100, 10);

        Map<String, Map<String, Object>> request = saveRequestFromCurrentSave(account);
        request.get("currency").put("currencyGold", 999_999_999);
        putAs(account, SAVE_URL, request).andExpect(status().isOk());

        assertThat(entries(account)).isEmpty();
    }

    @Test
    @DisplayName("여러 기능을 섞어 쓴 뒤 시작 잔액에 원장 증감을 더하면 현재 잔액과 같다")
    void ledgerReconcilesWithBalance() throws Exception {
        TestAccount account = createAccount();
        long inventoryId = giveEquipment(account);
        int startGold = 0;
        int startGem = 100;
        setGoldAndGem(account, startGold, startGem);

        battleWithGold(account, 100);
        postAs(account, PURCHASE_URL, Map.of("productId", GOLD_PACK)).andExpect(status().isOk());
        postAs(account, UPGRADE_URL).andExpect(status().isOk());
        postAs(account, levelUpUrl(inventoryId)).andExpect(status().isOk());

        int gold = assertChain(account, "GOLD", startGold);
        int gem = assertChain(account, "GEM", startGem);
        assertThat(gold).isEqualTo(balance(account, "currency_gold"));
        assertThat(gem).isEqualTo(balance(account, "currency_gem"));
    }

    @Test
    @DisplayName("구매를 동시에 보내도 원장이 빠짐없이 차례대로 이어지고 잔액과 맞는다")
    void concurrentPurchasesKeepLedgerChain() throws Exception {
        TestAccount account = createAccount();
        ShopProductData product = staticDataService.getShopProducts().get(GOLD_PACK);
        int count = 20;
        int startGem = product.priceGem() * count;
        setGoldAndGem(account, 0, startGem);

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tasks.add(() -> postAs(account, PURCHASE_URL, Map.of("productId", GOLD_PACK))
                    .andReturn().getResponse().getStatus());
        }
        assertThat(runConcurrently(tasks)).containsOnly(200);

        assertThat(entries(account)).hasSize(count * 2);
        assertThat(assertChain(account, "GEM", startGem)).isZero();
        assertThat(assertChain(account, "GOLD", 0)).isEqualTo(product.rewardAmount() * count);
    }

    // 한 재화의 원장을 처음부터 따라가며, 매 줄의 변경 후 잔액이 "이전 잔액 + 증감"과 같은지 확인하고 마지막 잔액을 돌려준다
    private int assertChain(TestAccount account, String type, int startBalance) {
        int expected = startBalance;
        for (Map<String, Object> row : entries(account)) {
            if (!type.equals(row.get("currency_type"))) {
                continue;
            }
            expected += ((Number) row.get("delta")).intValue();
            assertThat(((Number) row.get("balance_after")).intValue()).isEqualTo(expected);
        }
        return expected;
    }

    private void assertRow(Map<String, Object> row, String type, long delta, int balanceAfter,
                           String reason, long refId) {
        assertThat(row.get("currency_type")).isEqualTo(type);
        assertThat(((Number) row.get("delta")).longValue()).isEqualTo(delta);
        assertThat(((Number) row.get("balance_after")).intValue()).isEqualTo(balanceAfter);
        assertThat(row.get("reason")).isEqualTo(reason);
        assertThat(((Number) row.get("ref_id")).longValue()).isEqualTo(refId);
    }

    // 원장은 저장 순서(id)대로 읽는다. 재화 행을 잠근 트랜잭션 안에서 기록되므로 id 순서가 곧 처리 순서다
    private List<Map<String, Object>> entries(TestAccount account) {
        return jdbcTemplate.queryForList(
                "SELECT currency_type, delta, balance_after, reason, ref_id "
                        + "FROM currency_ledger WHERE player_id = ? ORDER BY id",
                account.playerId());
    }

    private int balance(TestAccount account, String column) {
        return jdbcTemplate.queryForObject(
                "SELECT " + column + " FROM currency WHERE player_id = ?", Integer.class, account.playerId());
    }

    // 골드를 받는 전투 결과를 제출하고 전투 ID를 돌려준다
    private long battleWithGold(TestAccount account, int gold) throws Exception {
        String enterBody = bodyOf(postAs(account, "/api/battles",
                Map.of("stageId", staticDataService.getFirstStageId())).andExpect(status().isOk()));
        Number battleId = JsonPath.read(enterBody, "$.battleId");

        Map<String, Object> result = new HashMap<>();
        result.put("victory", true);
        result.put("seconds", 3);
        result.put("kills", 0);
        result.put("gold", gold);
        result.put("rewardBoxes", 0);
        result.put("quality", 0);
        result.put("skillKills", Map.of());
        result.put("skillUpgrades", Map.of());
        result.put("pickups", Map.of());
        result.put("luckTrains", 0);

        postAs(account, "/api/battles/" + battleId.longValue() + "/result", result)
                .andExpect(status().isOk());
        return battleId.longValue();
    }

    private long giveEquipment(TestAccount account) {
        ItemData item = staticDataService.getItems().values().stream()
                .filter(i -> i.grade() == ItemGrade.GENERAL)
                .min(Comparator.comparing(ItemData::itemId))
                .orElseThrow();
        return equipmentRepository.save(Equipment.create(account.playerId(), item)).getId();
    }

    private String levelUpUrl(long inventoryId) {
        return INVENTORY_URL + "/" + inventoryId + "/levelup";
    }
}