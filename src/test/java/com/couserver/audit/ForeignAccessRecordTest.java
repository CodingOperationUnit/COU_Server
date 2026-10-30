package com.couserver.audit;

import com.couserver.inventory.entity.Equipment;
import com.couserver.inventory.entity.ItemGrade;
import com.couserver.inventory.repository.EquipmentRepository;
import com.couserver.staticdata.dto.ItemData;
import com.couserver.staticdata.service.StaticDataService;
import com.couserver.support.IntegrationTestSupport;
import com.couserver.support.TestAccount;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 남의 전투·장비·친구 요청에 접근하는 요청은 403이 되고 의심 활동으로 기록된다. 기록은 별도 스레드에서 저장된다
class ForeignAccessRecordTest extends IntegrationTestSupport {
    private static final Duration WAIT = Duration.ofSeconds(5);
    private static final String TYPE = "FOREIGN_RESOURCE_ACCESS";
    private static final String REQUESTS_URL = "/api/friends/requests";

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Test
    @DisplayName("남의 전투에 결과를 제출하면 403이고 요청한 쪽에 기록된다")
    void foreignBattleIsRecorded() throws Exception {
        TestAccount owner = createAccount();
        TestAccount other = createAccount();
        String enterBody = bodyOf(postAs(owner, "/api/battles",
                Map.of("stageId", staticDataService.getFirstStageId())).andExpect(status().isOk()));
        Number battleId = JsonPath.read(enterBody, "$.battleId");

        postAs(other, "/api/battles/" + battleId.longValue() + "/result", emptyResult())
                .andExpect(status().isForbidden());

        await().atMost(WAIT).untilAsserted(() ->
                assertThat(details(other)).anySatisfy(detail -> assertThat(detail).contains("battle")));
        assertThat(details(owner)).isEmpty();
    }

    @Test
    @DisplayName("남의 장비를 강화하려 하면 403이고 요청한 쪽에 기록된다")
    void foreignEquipmentIsRecorded() throws Exception {
        TestAccount owner = createAccount();
        TestAccount other = createAccount();
        long inventoryId = giveEquipment(owner);

        postAs(other, "/api/inventory/" + inventoryId + "/levelup").andExpect(status().isForbidden());

        await().atMost(WAIT).untilAsserted(() ->
                assertThat(details(other)).anySatisfy(detail -> assertThat(detail).contains("inventory")));
        assertThat(details(owner)).isEmpty();
    }

    @Test
    @DisplayName("남에게 온 친구 요청을 수락하려 하면 403이고 요청한 쪽에 기록된다")
    void foreignFriendRequestIsRecorded() throws Exception {
        TestAccount sender = createAccount();
        TestAccount receiver = createAccount();
        TestAccount stranger = createAccount();
        postAs(sender, REQUESTS_URL, Map.of("targetPlayerId", receiver.playerId())).andExpect(status().isOk());
        String body = bodyOf(getAs(receiver, REQUESTS_URL).andExpect(status().isOk()));
        Number requestId = JsonPath.read(body, "$[0].requestId");

        postAs(stranger, REQUESTS_URL + "/" + requestId.longValue() + "/accept").andExpect(status().isForbidden());

        await().atMost(WAIT).untilAsserted(() ->
                assertThat(details(stranger)).anySatisfy(detail -> assertThat(detail).contains("friendRequest")));
    }

    @Test
    @DisplayName("자기 전투·장비·받은 요청을 쓰는 정상 요청은 기록되지 않는다")
    void ownResourcesAreNotRecorded() throws Exception {
        TestAccount account = createAccount();
        TestAccount friend = createAccount();
        String enterBody = bodyOf(postAs(account, "/api/battles",
                Map.of("stageId", staticDataService.getFirstStageId())).andExpect(status().isOk()));
        Number battleId = JsonPath.read(enterBody, "$.battleId");
        postAs(account, "/api/battles/" + battleId.longValue() + "/result", emptyResult()).andExpect(status().isOk());
        long inventoryId = giveEquipment(account);
        setGoldAndGem(account, 1000, 0);
        postAs(account, "/api/inventory/" + inventoryId + "/levelup").andExpect(status().isOk());
        postAs(friend, REQUESTS_URL, Map.of("targetPlayerId", account.playerId())).andExpect(status().isOk());
        String body = bodyOf(getAs(account, REQUESTS_URL).andExpect(status().isOk()));
        Number requestId = JsonPath.read(body, "$[0].requestId");
        postAs(account, REQUESTS_URL + "/" + requestId.longValue() + "/accept").andExpect(status().isOk());

        // 기록이 생기지 않는 것을 확인하려면 비동기 저장이 끝날 시간을 둔 뒤 확인한다
        await().during(Duration.ofSeconds(1)).atMost(WAIT).untilAsserted(() ->
                assertThat(details(account)).isEmpty());
    }

    private Map<String, Object> emptyResult() {
        Map<String, Object> request = new HashMap<>();
        request.put("victory", false);
        request.put("seconds", 0);
        request.put("kills", 0);
        request.put("gold", 0);
        request.put("rewardBoxes", 0);
        request.put("skillKills", Map.of());
        request.put("skillUpgrades", Map.of());
        request.put("pickups", Map.of());
        request.put("luckTrains", 0);
        return request;
    }

    private long giveEquipment(TestAccount account) {
        ItemData item = staticDataService.getItems().values().stream()
                .filter(i -> i.grade() == ItemGrade.GENERAL)
                .min(Comparator.comparing(ItemData::itemId))
                .orElseThrow();
        return equipmentRepository.save(Equipment.create(account.playerId(), item)).getId();
    }

    private List<String> details(TestAccount account) {
        return jdbcTemplate.queryForList(
                "SELECT detail FROM suspicious_activity WHERE player_id = ? AND activity_type = ?",
                String.class, account.playerId(), TYPE);
    }
}