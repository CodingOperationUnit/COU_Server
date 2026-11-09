package com.couserver.friend;

import com.couserver.support.IntegrationTestSupport;
import com.couserver.support.TestAccount;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FriendApiTest extends IntegrationTestSupport {
    private static final String FRIENDS_URL = "/api/friends";
    private static final String REQUESTS_URL = "/api/friends/requests";

    @Test
    @DisplayName("계정 ID로 검색하면 상대가 관계 NONE으로 보인다")
    void searchByAccountLoginId() throws Exception {
        TestAccount me = createAccount();
        TestAccount target = createAccount();

        getAs(me, FRIENDS_URL + "/search?keyword=" + target.loginId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].accountLoginId", hasItem(target.loginId())))
                .andExpect(jsonPath("$[?(@.playerId == %d)].relation", target.playerId())
                        .value(contains("NONE")));
    }

    @Test
    @DisplayName("친구 요청을 수락하면 양쪽 친구 목록에 서로 보인다")
    void requestAndAccept() throws Exception {
        TestAccount sender = createAccount();
        TestAccount receiver = createAccount();

        sendRequest(sender, receiver)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relation").value("REQUEST_SENT"));

        getAs(receiver, REQUESTS_URL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].playerId").value(sender.playerId()));

        postAs(receiver, REQUESTS_URL + "/" + firstReceivedRequestId(receiver) + "/accept")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").value(sender.playerId()));

        getAs(sender, FRIENDS_URL)
                .andExpect(jsonPath("$.friends[*].playerId", hasItem(receiver.playerId().intValue())));
        getAs(receiver, FRIENDS_URL)
                .andExpect(jsonPath("$.friends[*].playerId", hasItem(sender.playerId().intValue())));
    }

    @Test
    @DisplayName("같은 상대에게 다시 요청하면 409")
    void duplicateRequestFails() throws Exception {
        TestAccount sender = createAccount();
        TestAccount receiver = createAccount();
        sendRequest(sender, receiver).andExpect(status().isOk());

        sendRequest(sender, receiver)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REQUEST_ALREADY_SENT"));
    }

    @Test
    @DisplayName("자기 자신에게는 요청할 수 없다")
    void requestToSelfFails() throws Exception {
        TestAccount me = createAccount();

        sendRequest(me, me)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_REQUEST_SELF"));
    }

    @Test
    @DisplayName("거절하면 요청이 사라지고 다시 요청할 수 있다")
    void rejectRequest() throws Exception {
        TestAccount sender = createAccount();
        TestAccount receiver = createAccount();
        sendRequest(sender, receiver).andExpect(status().isOk());

        postAs(receiver, REQUESTS_URL + "/" + firstReceivedRequestId(receiver) + "/reject")
                .andExpect(status().isNoContent());

        getAs(receiver, REQUESTS_URL)
                .andExpect(jsonPath("$").isEmpty());
        sendRequest(sender, receiver).andExpect(status().isOk());
    }

    @Test
    @DisplayName("보낸 쪽은 자기 요청을 수락할 수 없다")
    void senderCannotAccept() throws Exception {
        TestAccount sender = createAccount();
        TestAccount receiver = createAccount();
        sendRequest(sender, receiver).andExpect(status().isOk());
        long requestId = firstReceivedRequestId(receiver);

        postAs(sender, REQUESTS_URL + "/" + requestId + "/accept")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_REQUEST_RECEIVER"));
    }

    @Test
    @DisplayName("친구를 삭제하면 양쪽 목록에서 사라진다")
    void removeFriend() throws Exception {
        TestAccount sender = createAccount();
        TestAccount receiver = createAccount();
        sendRequest(sender, receiver).andExpect(status().isOk());
        postAs(receiver, REQUESTS_URL + "/" + firstReceivedRequestId(receiver) + "/accept")
                .andExpect(status().isOk());

        deleteAs(sender, FRIENDS_URL + "/" + receiver.playerId())
                .andExpect(status().isNoContent());

        getAs(sender, FRIENDS_URL).andExpect(jsonPath("$.friends").isEmpty());
        getAs(receiver, FRIENDS_URL).andExpect(jsonPath("$.friends").isEmpty());
    }

    @Test
    @DisplayName("친구가 메시지를 보낸 적이 없으면 lastReceivedMessageId는 0")
    void lastReceivedMessageIdWithoutMessages() throws Exception {
        TestAccount me = createAccount();
        TestAccount friend = createAccount();
        makeFriends(me, friend);

        expectLastReceived(me, friend, 0);
    }

    @Test
    @DisplayName("친구가 보낸 마지막 메시지 ID가 오고, 그 뒤에 내가 보낸 메시지는 세지 않는다")
    void lastReceivedMessageIdIgnoresMine() throws Exception {
        TestAccount me = createAccount();
        TestAccount friend = createAccount();
        makeFriends(me, friend);

        sendMessage(friend, me);
        int last = sendMessage(friend, me);
        sendMessage(me, friend);

        expectLastReceived(me, friend, last);
    }

    @Test
    @DisplayName("친구마다 따로 오고, 내 ID가 쌍의 낮은 쪽이든 높은 쪽이든 같다")
    void lastReceivedMessageIdPerFriend() throws Exception {
        // 가입 순서대로 ID가 커진다
        TestAccount lowFriend = createAccount();
        TestAccount me = createAccount();
        TestAccount highFriend = createAccount();
        assertThat(lowFriend.playerId()).isLessThan(me.playerId());
        assertThat(highFriend.playerId()).isGreaterThan(me.playerId());
        makeFriends(me, lowFriend);
        makeFriends(me, highFriend);

        int fromLow = sendMessage(lowFriend, me);
        int fromHigh = sendMessage(highFriend, me);

        expectLastReceived(me, lowFriend, fromLow);
        expectLastReceived(me, highFriend, fromHigh);
    }

    private void makeFriends(TestAccount sender, TestAccount receiver) throws Exception {
        sendRequest(sender, receiver).andExpect(status().isOk());
        postAs(receiver, REQUESTS_URL + "/" + firstReceivedRequestId(receiver) + "/accept")
                .andExpect(status().isOk());
    }

    // 보낸 메시지의 ID
    private int sendMessage(TestAccount from, TestAccount to) throws Exception {
        String body = bodyOf(postAs(from, "/api/chat/" + to.playerId() + "/messages", Map.of("content", "hi"))
                .andExpect(status().isOk()));
        Number messageId = JsonPath.read(body, "$.messageId");
        return messageId.intValue();
    }

    private void expectLastReceived(TestAccount me, TestAccount friend, int messageId) throws Exception {
        getAs(me, FRIENDS_URL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friends[?(@.playerId == %d)].lastReceivedMessageId", friend.playerId())
                        .value(contains(messageId)));
    }

    private org.springframework.test.web.servlet.ResultActions sendRequest(TestAccount from, TestAccount to)
            throws Exception {
        return postAs(from, REQUESTS_URL, Map.of("targetPlayerId", to.playerId()));
    }

    // 새로 만든 계정은 받은 요청이 하나뿐이라 첫 번째 요청을 쓴다
    private long firstReceivedRequestId(TestAccount receiver) throws Exception {
        String body = bodyOf(getAs(receiver, REQUESTS_URL).andExpect(status().isOk()));
        Number requestId = JsonPath.read(body, "$[0].requestId");
        return requestId.longValue();
    }
}