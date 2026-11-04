package com.couserver.inventory;

import com.couserver.inventory.entity.Equipment;
import com.couserver.inventory.entity.ItemGrade;
import com.couserver.inventory.repository.EquipmentRepository;
import com.couserver.staticdata.dto.ItemData;
import com.couserver.staticdata.service.StaticDataService;
import com.couserver.support.IntegrationTestSupport;
import com.couserver.support.TestAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Comparator;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InventoryApiTest extends IntegrationTestSupport {
    private static final String INVENTORY_URL = "/api/inventory";
    private static final String SYNTHESIZE_ALL_URL = INVENTORY_URL + "/synthesize/all";
    private static final int LEVEL_UP_BASE_COST = 1000;   // InventoryService.LEVEL_UP_BASE_COST와 같은 값 (비용 = 1000 × 현재 레벨)

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Test
    @DisplayName("골드가 있으면 장비 레벨이 1 오르고 비용이 차감된다")
    void levelUp() throws Exception {
        TestAccount account = createAccount();
        long inventoryId = giveEquipment(account, generalItem());
        setGoldAndGem(account, LEVEL_UP_BASE_COST, 0);

        postAs(account, levelUpUrl(inventoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventoryItemLevel").value(2))
                .andExpect(jsonPath("$.spentGold").value(LEVEL_UP_BASE_COST))
                .andExpect(jsonPath("$.currencyGold").value(0));

        getAs(account, INVENTORY_URL)
                .andExpect(jsonPath("$.items[0].inventoryItemLevel").value(2))
                .andExpect(jsonPath("$.currencyGold").value(0));
    }

    @Test
    @DisplayName("골드가 부족하면 레벨업은 400이고 레벨과 골드는 그대로다")
    void levelUpWithoutEnoughGoldFails() throws Exception {
        TestAccount account = createAccount();
        long inventoryId = giveEquipment(account, generalItem());
        setGoldAndGem(account, LEVEL_UP_BASE_COST - 1, 0);

        postAs(account, levelUpUrl(inventoryId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_ENOUGH_GOLD"));

        getAs(account, INVENTORY_URL)
                .andExpect(jsonPath("$.items[0].inventoryItemLevel").value(1))
                .andExpect(jsonPath("$.currencyGold").value(LEVEL_UP_BASE_COST - 1));
    }

    @Test
    @DisplayName("일괄 레벨업은 골드가 허락하는 만큼 레벨을 올린다")
    void levelUpBatch() throws Exception {
        TestAccount account = createAccount();
        long inventoryId = giveEquipment(account, generalItem());
        int costToLevel3 = LEVEL_UP_BASE_COST * 1 + LEVEL_UP_BASE_COST * 2;   // 1→2, 2→3
        int leftover = 500;                                                  // 3→4 비용(3000)에는 모자란 금액
        setGoldAndGem(account, costToLevel3 + leftover, 0);

        postAs(account, levelUpUrl(inventoryId) + "/batch")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventoryItemLevel").value(3))
                .andExpect(jsonPath("$.levelsGained").value(2))
                .andExpect(jsonPath("$.spentGold").value(costToLevel3))
                .andExpect(jsonPath("$.currencyGold").value(leftover));
    }

    @Test
    @DisplayName("같은 장비 3개로 합성하면 대상 등급이 오르고 재료 2개가 사라진다")
    void synthesize() throws Exception {
        TestAccount account = createAccount();
        ItemData item = generalItem();
        long targetId = giveEquipment(account, item);
        long material1 = giveEquipment(account, item);
        long material2 = giveEquipment(account, item);

        postAs(account, INVENTORY_URL + "/" + targetId + "/synthesize")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventoryId").value(targetId))
                .andExpect(jsonPath("$.inventoryItemGrade").value(ItemGrade.UNCOMMON.toClientName()))
                .andExpect(jsonPath("$.consumedInventoryIds",
                        containsInAnyOrder((int) material1, (int) material2)));

        getAs(account, INVENTORY_URL)
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    @DisplayName("재료가 부족하면 합성은 400이고 장비는 그대로다")
    void synthesizeWithoutEnoughMaterialFails() throws Exception {
        TestAccount account = createAccount();
        ItemData item = generalItem();
        long targetId = giveEquipment(account, item);
        giveEquipment(account, item);

        postAs(account, INVENTORY_URL + "/" + targetId + "/synthesize")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_ENOUGH_MATERIAL"));

        getAs(account, INVENTORY_URL)
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    @DisplayName("장착한 장비는 합성 재료로 쓰지 않는다")
    void equippedItemIsNotUsedAsMaterial() throws Exception {
        TestAccount account = createAccount();
        ItemData item = generalItem();
        long targetId = giveEquipment(account, item);
        giveEquipment(account, item);
        long equippedId = giveEquipment(account, item);

        postAs(account, INVENTORY_URL + "/" + equippedId + "/equip")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipped.inventoryId").value(equippedId))
                .andExpect(jsonPath("$.unequipped").value(nullValue()));

        postAs(account, INVENTORY_URL + "/" + targetId + "/synthesize")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_ENOUGH_MATERIAL"));
    }

    @Test
    @DisplayName("합성하면 대상은 세 장비 중 가장 높은 레벨을 이어받는다")
    void synthesizeInheritsHighestLevel() throws Exception {
        TestAccount account = createAccount();
        ItemData item = generalItem();
        long targetId = giveEquipment(account, item, 2);
        giveEquipment(account, item, 5);
        giveEquipment(account, item, 3);

        postAs(account, INVENTORY_URL + "/" + targetId + "/synthesize")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventoryItemLevel").value(5));
    }

    @Test
    @DisplayName("일괄 합성은 같은 장비 9개를 레어 1개로 연쇄 합성한다")
    void synthesizeAllChainsToRare() throws Exception {
        TestAccount account = createAccount();
        ItemData item = generalItem();
        long firstId = giveEquipment(account, item);
        for (int i = 1; i < 9; i++) {
            giveEquipment(account, item);
        }

        postAs(account, SYNTHESIZE_ALL_URL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synthesizedItems.length()").value(1))
                .andExpect(jsonPath("$.synthesizedItems[0].inventoryId").value(firstId))
                .andExpect(jsonPath("$.synthesizedItems[0].inventoryItemGrade").value(ItemGrade.RARE.toClientName()))
                .andExpect(jsonPath("$.consumedInventoryIds.length()").value(8));

        getAs(account, INVENTORY_URL)
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    @DisplayName("일괄 합성은 장착한 장비를 대상으로 남긴다")
    void synthesizeAllKeepsEquippedItemAsTarget() throws Exception {
        TestAccount account = createAccount();
        ItemData item = generalItem();
        giveEquipment(account, item);
        giveEquipment(account, item);
        long equippedId = giveEquipment(account, item);

        postAs(account, INVENTORY_URL + "/" + equippedId + "/equip")
                .andExpect(status().isOk());

        postAs(account, SYNTHESIZE_ALL_URL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synthesizedItems.length()").value(1))
                .andExpect(jsonPath("$.synthesizedItems[0].inventoryId").value(equippedId))
                .andExpect(jsonPath("$.synthesizedItems[0].inventoryItemGrade").value(ItemGrade.UNCOMMON.toClientName()))
                .andExpect(jsonPath("$.synthesizedItems[0].isEquipped").value(true));
    }

    @Test
    @DisplayName("일괄 합성은 장비 종류마다 합성하고, 묶음이 안 되는 장비는 남긴다")
    void synthesizeAllPerItem() throws Exception {
        TestAccount account = createAccount();
        List<ItemData> items = generalItems(2);
        long firstA = giveEquipment(account, items.get(0));
        giveEquipment(account, items.get(0));
        giveEquipment(account, items.get(0));
        giveEquipment(account, items.get(0));   // 남는 장비
        long firstB = giveEquipment(account, items.get(1));
        giveEquipment(account, items.get(1), 4);
        giveEquipment(account, items.get(1));

        postAs(account, SYNTHESIZE_ALL_URL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synthesizedItems.length()").value(2))
                .andExpect(jsonPath("$.synthesizedItems[0].inventoryId").value(firstA))
                .andExpect(jsonPath("$.synthesizedItems[1].inventoryId").value(firstB))
                .andExpect(jsonPath("$.synthesizedItems[1].inventoryItemLevel").value(4))
                .andExpect(jsonPath("$.consumedInventoryIds.length()").value(4));

        getAs(account, INVENTORY_URL)
                .andExpect(jsonPath("$.items.length()").value(3));
    }

    @Test
    @DisplayName("합성할 묶음이 없으면 일괄 합성은 400이고 장비는 그대로다")
    void synthesizeAllWithoutEnoughMaterialFails() throws Exception {
        TestAccount account = createAccount();
        ItemData item = generalItem();
        giveEquipment(account, item);
        giveEquipment(account, item);

        postAs(account, SYNTHESIZE_ALL_URL)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_ENOUGH_MATERIAL"));

        getAs(account, INVENTORY_URL)
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    @DisplayName("다른 계정의 장비는 레벨업할 수 없다")
    void levelUpOthersEquipmentFails() throws Exception {
        TestAccount owner = createAccount();
        TestAccount other = createAccount();
        long inventoryId = giveEquipment(owner, generalItem());
        setGoldAndGem(other, LEVEL_UP_BASE_COST, 0);

        postAs(other, levelUpUrl(inventoryId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_OWNED_INVENTORY"));
    }

    // 테스트 준비용: 장비를 DB에 직접 넣는다 (상점·전투 보상은 무작위라 합성 조건을 맞출 수 없다)
    private long giveEquipment(TestAccount account, ItemData item) {
        return equipmentRepository.save(Equipment.create(account.playerId(), item)).getId();
    }

    private long giveEquipment(TestAccount account, ItemData item, int level) {
        Equipment equipment = Equipment.create(account.playerId(), item);
        for (int i = 1; i < level; i++) {
            equipment.levelUp();
        }
        return equipmentRepository.save(equipment).getId();
    }

    // 기본 등급이 General인 장비 중 ID가 가장 작은 것
    private ItemData generalItem() {
        return staticDataService.getItems().values().stream()
                .filter(item -> item.grade() == ItemGrade.COMMON)
                .min(Comparator.comparing(ItemData::itemId))
                .orElseThrow();
    }

    // 기본 등급이 General인 장비를 ID가 작은 순서로 count개
    private List<ItemData> generalItems(int count) {
        return staticDataService.getItems().values().stream()
                .filter(item -> item.grade() == ItemGrade.COMMON)
                .sorted(Comparator.comparing(ItemData::itemId))
                .limit(count)
                .toList();
    }

    private String levelUpUrl(long inventoryId) {
        return INVENTORY_URL + "/" + inventoryId + "/levelup";
    }
}