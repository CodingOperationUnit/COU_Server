package com.couserver.ledger;

import com.couserver.account.entity.Currency;
import com.couserver.account.repository.CurrencyRepository;
import com.couserver.ledger.entity.LedgerReason;
import com.couserver.ledger.service.CurrencyLedgerService;
import com.couserver.support.IntegrationTestSupport;
import com.couserver.support.TestAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 4-a-1: 원장 서비스 자체의 동작. 각 기능과의 연결은 4-a-2에서 확인한다
class CurrencyLedgerServiceTest extends IntegrationTestSupport {

    @Autowired
    private CurrencyLedgerService ledger;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("트랜잭션 밖에서 부르면 오류가 나고 재화는 바뀌지 않는다")
    void outsideTransactionFails() {
        Currency currency = new Currency(1L, 0, 0, 60, Instant.EPOCH);

        assertThatThrownBy(() -> ledger.addGold(currency, 10, LedgerReason.BATTLE_RESULT, null))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(currency.getCurrencyGold()).isZero();
    }

    @Test
    @DisplayName("지급하면 증감, 변경 후 잔액, 사유, 관련 ID가 기록된다")
    void recordsAdd() throws Exception {
        TestAccount account = createAccount();
        setGoldAndGem(account, 100, 0);

        inTransaction(account, currency -> ledger.addGold(currency, 250, LedgerReason.BATTLE_RESULT, 77L));

        assertThat(entries(account)).singleElement().satisfies(row -> {
            assertThat(row.get("currency_type")).isEqualTo("GOLD");
            assertThat(((Number) row.get("delta")).longValue()).isEqualTo(250L);
            assertThat(((Number) row.get("balance_after")).intValue()).isEqualTo(350);
            assertThat(row.get("reason")).isEqualTo("BATTLE_RESULT");
            assertThat(((Number) row.get("ref_id")).longValue()).isEqualTo(77L);
        });
        assertThat(balance(account, "currency_gold")).isEqualTo(350);
    }

    @Test
    @DisplayName("사용하면 음수 증감으로 기록된다")
    void recordsSpendAsNegativeDelta() throws Exception {
        TestAccount account = createAccount();
        setGoldAndGem(account, 0, 50);

        inTransaction(account, currency -> ledger.spendGem(currency, 30, LedgerReason.SHOP_PURCHASE, 50011L));

        assertThat(entries(account)).singleElement().satisfies(row -> {
            assertThat(row.get("currency_type")).isEqualTo("GEM");
            assertThat(((Number) row.get("delta")).longValue()).isEqualTo(-30L);
            assertThat(((Number) row.get("balance_after")).intValue()).isEqualTo(20);
        });
        assertThat(balance(account, "currency_gem")).isEqualTo(20);
    }

    @Test
    @DisplayName("증감이 0이면 기록하지 않는다")
    void zeroDeltaIsNotRecorded() throws Exception {
        TestAccount account = createAccount();

        inTransaction(account, currency -> ledger.addGold(currency, 0, LedgerReason.BATTLE_RESULT, 1L));

        assertThat(entries(account)).isEmpty();
    }

    @Test
    @DisplayName("트랜잭션이 롤백되면 원장 기록과 재화 변경이 함께 사라진다")
    void rollbackRemovesBoth() throws Exception {
        TestAccount account = createAccount();
        setGoldAndGem(account, 100, 0);

        assertThatThrownBy(() -> inTransaction(account, currency -> {
            ledger.addGold(currency, 250, LedgerReason.SHOP_PURCHASE, 1L);
            throw new IllegalStateException("의도한 실패");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(entries(account)).isEmpty();
        assertThat(balance(account, "currency_gold")).isEqualTo(100);
    }

    @Test
    @DisplayName("최대값에서 멈춘 경우에는 실제로 늘어난 양만 기록한다")
    void recordsActualIncreaseWhenCapped() throws Exception {
        TestAccount account = createAccount();
        setGoldAndGem(account, Integer.MAX_VALUE - 10, 0);

        inTransaction(account, currency -> ledger.addGold(currency, 100, LedgerReason.BATTLE_RESULT, 1L));

        assertThat(entries(account)).singleElement().satisfies(row -> {
            assertThat(((Number) row.get("delta")).longValue()).isEqualTo(10L);
            assertThat(((Number) row.get("balance_after")).intValue()).isEqualTo(Integer.MAX_VALUE);
        });
    }

    // 실제 서비스처럼 재화 행을 잠근 트랜잭션 안에서 실행한다
    private void inTransaction(TestAccount account, java.util.function.Consumer<Currency> action) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Currency currency = currencyRepository.findForUpdate(account.playerId()).orElseThrow();
            action.accept(currency);
        });
    }

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
}