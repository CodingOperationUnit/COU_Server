package com.couserver.achievement.repository;

import com.couserver.achievement.entity.PlayerAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerAchievementRepository extends JpaRepository<PlayerAchievement, Long> {
    List<PlayerAchievement> findByPlayerId(Long playerId);
    Optional<PlayerAchievement> findByPlayerIdAndAchievementId(Long playerId, int achievementId);
}
