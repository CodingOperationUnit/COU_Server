package com.couserver.battle;

import com.couserver.battle.entity.BattleRecord;
import com.couserver.battle.event.BattleRecordListener;
import com.couserver.battle.event.BattleRecordedEvent;
import com.couserver.battle.repository.BattleRecordRepository;
import com.couserver.player.dto.PlayerFinalStatResponse;
import com.couserver.player.service.PlayerFinalStatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BattleRecordListenerTest {
    private static final long PLAYER_ID = 1L;
    private static final BattleRecordedEvent EVENT = new BattleRecordedEvent(
            PLAYER_ID, 10L, 1, true, true, 30L, 3, 5, 8, 100, 200, 0, 12, Instant.EPOCH);

    @Mock
    private BattleRecordRepository repository;

    @Mock
    private PlayerFinalStatService playerFinalStatService;

    @Mock
    private PlatformTransactionManager transactionManager;

    private BattleRecordListener listener;

    @BeforeEach
    void setUp() {
        listener = new BattleRecordListener(repository, playerFinalStatService, transactionManager);
    }

    @Test
    @DisplayName("DB 저장이 실패해도 예외가 밖으로 나가지 않는다")
    void saveFailureDoesNotPropagate() {
        when(playerFinalStatService.getFinalStat(PLAYER_ID)).thenReturn(finalStat(2000));
        when(repository.save(any())).thenThrow(new RuntimeException("DB down"));

        assertThatCode(() -> listener.onBattleRecorded(EVENT)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("전투력을 계산하지 못해도 나머지 값으로 기록하고 전투력은 -1로 남긴다")
    void statFailureStillRecords() {
        when(playerFinalStatService.getFinalStat(PLAYER_ID)).thenThrow(new IllegalStateException("없는 아이템"));

        listener.onBattleRecorded(EVENT);

        ArgumentCaptor<BattleRecord> saved = ArgumentCaptor.forClass(BattleRecord.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getCombatPower()).isEqualTo(-1);
        assertThat(saved.getValue().getMinDamage()).isEqualTo(-1);
        assertThat(saved.getValue().getGold()).isEqualTo(100);
        assertThat(saved.getValue().getGoldLimit()).isEqualTo(200);
    }

    private static PlayerFinalStatResponse finalStat(int combatPower) {
        return new PlayerFinalStatResponse(100, 1000, 0, 0, combatPower, 200, 5, 100, 9f, 16f, 2f, null);
    }
}