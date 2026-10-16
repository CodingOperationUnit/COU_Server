package com.couserver.friend.service;

import com.couserver.account.entity.PlayerProfile;
import com.couserver.account.repository.PlayerProfileRepository;
import com.couserver.audit.entity.SuspiciousActivityType;
import com.couserver.audit.service.SuspiciousActivityRecorder;
import com.couserver.common.exception.BusinessException;
import com.couserver.friend.dto.*;
import com.couserver.friend.entity.FriendRelation;
import com.couserver.friend.entity.Friendship;
import com.couserver.friend.entity.FriendshipStatus;
import com.couserver.friend.exception.FriendErrorCode;
import com.couserver.friend.repository.FriendshipRepository;
import com.couserver.presence.service.PresenceService;
import com.couserver.ranking.repository.RankingRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendService {
    public static final int MAX_FRIENDS = 50;
    public static final int MAX_RECEIVED_REQUESTS = 50;

    private final FriendshipRepository friendshipRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final RankingRedisRepository rankingRedisRepository;
    private final PresenceService presenceService;
    private final SuspiciousActivityRecorder suspiciousActivityRecorder;

    // 친구 요청
    @Transactional
    public FriendRequestResultResponse sendRequest(Long playerId, Long targetId) {
        if (playerId.equals(targetId)) {
            throw new BusinessException(FriendErrorCode.CANNOT_REQUEST_SELF);
        }
        if (!playerProfileRepository.existsById(targetId)) {
            throw new BusinessException(FriendErrorCode.PLAYER_NOT_FOUND);
        }

        Optional<Friendship> existing = friendshipRepository.findByPlayerLowIdAndPlayerHighId(
                Friendship.lowOf(playerId, targetId), Friendship.highOf(playerId, targetId));

        if (existing.isPresent()) {
            Friendship friendship = existing.get();
            if (!friendship.isPending()) {
                throw new BusinessException(FriendErrorCode.ALREADY_FRIENDS);
            }
            if (friendship.getRequestedBy().equals(playerId)) {
                throw new BusinessException(FriendErrorCode.REQUEST_ALREADY_SENT);
            }
            // 상대가 이미 나에게 요청을 보낸 상태: 서로 원하는 것이므로 바로 친구가 된다
            confirmFriendship(friendship, playerId);
            return new FriendRequestResultResponse(FriendRelation.FRIEND);
        }

        if (countFriends(playerId) >= MAX_FRIENDS) {
            throw new BusinessException(FriendErrorCode.FRIEND_LIMIT_EXCEEDED);
        }
        if (friendshipRepository.countReceivedRequests(targetId, FriendshipStatus.PENDING) >= MAX_RECEIVED_REQUESTS) {
            throw new BusinessException(FriendErrorCode.TARGET_REQUEST_LIMIT_EXCEEDED);
        }

        try {
            friendshipRepository.saveAndFlush(Friendship.request(playerId, targetId));
        } catch (DataIntegrityViolationException e) {
            // 같은 쌍으로 동시에 요청이 들어온 경우 (유니크 제약)
            throw new BusinessException(FriendErrorCode.REQUEST_ALREADY_SENT);
        }
        return new FriendRequestResultResponse(FriendRelation.REQUEST_SENT);
    }

    // 받은 요청 목록 (최신순)
    @Transactional(readOnly = true)
    public List<FriendRequestResponse> getReceivedRequests(Long playerId) {
        List<Friendship> requests = friendshipRepository.findReceivedRequests(playerId, FriendshipStatus.PENDING);
        Map<Long, PlayerProfile> profiles = profilesOf(requests.stream().map(Friendship::getRequestedBy).toList());

        return requests.stream()
                .sorted(Comparator.comparing(Friendship::getCreatedAt, Comparator.reverseOrder()))
                .map(f -> {
                    PlayerProfile p = profiles.get(f.getRequestedBy());
                    return p == null ? null
                            : new FriendRequestResponse(f.getFriendshipId(), p.getPlayerId(), p.getPlayerNickname(), p.getAccountLevel());
                })
                .filter(Objects::nonNull)
                .toList();
    }

    // 수락
    @Transactional
    public FriendResponse acceptRequest(Long playerId, Long requestId) {
        Friendship friendship = getReceivedRequest(playerId, requestId);
        confirmFriendship(friendship, playerId);

        PlayerProfile requester = playerProfileRepository.findById(friendship.getRequestedBy())
                .orElseThrow(() -> new BusinessException(FriendErrorCode.PLAYER_NOT_FOUND));
        return toFriendResponse(requester, presenceService.isOnline(requester.getPlayerId()));
    }

    // 거절: 행을 지운다. 그래서 상대가 다시 요청할 수 있다
    @Transactional
    public void rejectRequest(Long playerId, Long requestId) {
        friendshipRepository.delete(getReceivedRequest(playerId, requestId));
    }

    // 친구 목록 (온라인 순, 그다음이 닉네임순으로 정렬)
    @Transactional(readOnly = true)
    public FriendListResponse getFriends(Long playerId) {
        List<Friendship> list = friendshipRepository.findByPlayerAndStatus(playerId, FriendshipStatus.ACCEPTED);
        List<Long> friendIds = list.stream().map(f -> f.otherOf(playerId)).toList();
        Map<Long, PlayerProfile> profiles = profilesOf(friendIds);
        Set<Long> online = presenceService.onlineAmong(friendIds);

        List<FriendResponse> friends = friendIds.stream()
                .map(profiles::get)
                .filter(Objects::nonNull)
                .map(p -> toFriendResponse(p, online.contains(p.getPlayerId())))
                .sorted(Comparator.comparing(FriendResponse::online, Comparator.reverseOrder())
                        .thenComparing(FriendResponse::nickname))
                .toList();
        return new FriendListResponse(friends, MAX_FRIENDS);
    }

    // 친구 삭제
    @Transactional
    public void removeFriend(Long playerId, Long friendPlayerId) {
        Friendship friendship = friendshipRepository.findByPlayerLowIdAndPlayerHighId(
                        Friendship.lowOf(playerId, friendPlayerId), Friendship.highOf(playerId, friendPlayerId))
                .filter(f -> !f.isPending())
                .orElseThrow(() -> new BusinessException(FriendErrorCode.NOT_FRIENDS));
        friendshipRepository.delete(friendship);
    }

    // 검색: 닉네임또는 플레이어 ID
    @Transactional(readOnly = true)
    public List<FriendSearchResponse> search(Long playerId, String keyword) {
        String key = keyword == null ? "" : keyword.trim();
        if (key.isEmpty()) {
            return List.of();
        }

        Map<Long, PlayerProfile> found = new LinkedHashMap<>();
        if (key.length() <= 18 && key.chars().allMatch(Character::isDigit)) {   // 숫자면 플레이어 ID로도 찾는다
            playerProfileRepository.findById(Long.parseLong(key)).ifPresent(p -> found.put(p.getPlayerId(), p));
        }
        for (PlayerProfile p : playerProfileRepository
                .findTop20ByPlayerNicknameContainingIgnoreCaseOrderByPlayerNicknameAsc(key)) {
            found.putIfAbsent(p.getPlayerId(), p);
        }
        found.remove(playerId);   // 나 자신은 제외

        Map<Long, FriendRelation> relations = new HashMap<>();
        for (Friendship f : friendshipRepository.findAllOf(playerId)) {
            relations.put(f.otherOf(playerId), f.relationOf(playerId));
        }

        return found.values().stream()
                .map(p -> new FriendSearchResponse(p.getPlayerId(), p.getPlayerNickname(), p.getAccountLevel(),
                        relations.getOrDefault(p.getPlayerId(), FriendRelation.NONE)))
                .toList();
    }

    // 내가 받은 요청인지 확인하고 가져온다
    private Friendship getReceivedRequest(Long playerId, Long requestId) {
        Friendship friendship = friendshipRepository.findById(requestId)
                .filter(Friendship::isPending)
                .orElseThrow(() -> new BusinessException(FriendErrorCode.REQUEST_NOT_FOUND));
        if (!friendship.receiverId().equals(playerId)) {
            // 정상 클라이언트의 수락·거절 버튼은 받은 요청에만 있다
            suspiciousActivityRecorder.record(playerId, SuspiciousActivityType.FOREIGN_RESOURCE_ACCESS,
                    "friendRequest requestId=" + requestId);
            throw new BusinessException(FriendErrorCode.NOT_REQUEST_RECEIVER);
        }
        return friendship;
    }

    // 친구 성립: 양쪽 친구 수를 확인하고 수락 처리
    private void confirmFriendship(Friendship friendship, Long accepterId) {
        if (countFriends(accepterId) >= MAX_FRIENDS) {
            throw new BusinessException(FriendErrorCode.FRIEND_LIMIT_EXCEEDED);
        }
        if (countFriends(friendship.otherOf(accepterId)) >= MAX_FRIENDS) {
            throw new BusinessException(FriendErrorCode.TARGET_FRIEND_LIMIT_EXCEEDED);
        }
        friendship.accept();
    }

    private long countFriends(Long playerId) {
        return friendshipRepository.countByPlayerAndStatus(playerId, FriendshipStatus.ACCEPTED);
    }

    private Map<Long, PlayerProfile> profilesOf(List<Long> playerIds) {
        return playerProfileRepository.findAllById(playerIds).stream()
                .collect(Collectors.toMap(PlayerProfile::getPlayerId, Function.identity()));
    }

    private FriendResponse toFriendResponse(PlayerProfile p, boolean online) {
        Long combatPower = rankingRedisRepository.value(RankingRedisRepository.COMBAT_POWER_KEY, p.getPlayerId());
        return new FriendResponse(p.getPlayerId(), p.getPlayerNickname(), p.getAccountLevel(),
                combatPower == null ? 0L : combatPower, online);
    }
}