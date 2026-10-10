package com.ticketon.ticketon.payment.implement.gateway;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PgUnavailablePredicateTest {

    private final PgUnavailablePredicate predicate = new PgUnavailablePredicate();

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "PG_UNAVAILABLE,          true",
            "PAYMENT_REJECTED,        false",
            "PAYMENT_RESULT_UNKNOWN,  false",
    })
    @DisplayName("PG_UNAVAILABLE 만 서킷 실패로 집계한다")
    void recordsOnlyPgUnavailable(ErrorType errorType, boolean expected) {
        assertThat(predicate.test(new AppException(errorType))).isEqualTo(expected);
    }

    @Test
    @DisplayName("AppException 이 아닌 예외는 집계하지 않는다")
    void ignoresOtherExceptions() {
        assertThat(predicate.test(new IllegalStateException())).isFalse();
    }
}
