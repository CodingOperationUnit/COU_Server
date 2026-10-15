package com.couserver.audit;

import com.couserver.support.IntegrationTestSupport;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

// enum 값을 추가해도 ddl-auto=update가 컬럼을 바꾸지 못하므로, 값이 늘어날 수 있는 enum 컬럼은 varchar여야 한다
class EnumColumnTypeTest extends IntegrationTestSupport {

    @ParameterizedTest(name = "{0}.{1}")
    @CsvSource({
            "suspicious_activity, activity_type",
            "currency_ledger, currency_type",
            "currency_ledger, reason"
    })
    @DisplayName("값이 늘어날 수 있는 enum 컬럼은 varchar로 만들어진다")
    void enumColumnsAreVarchar(String table, String column) {
        String dataType = jdbcTemplate.queryForObject(
                "SELECT DATA_TYPE FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                String.class, table, column);

        assertThat(dataType).isEqualToIgnoringCase("varchar");
    }
}