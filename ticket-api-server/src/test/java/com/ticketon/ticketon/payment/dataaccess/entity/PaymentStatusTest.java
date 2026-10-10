package com.ticketon.ticketon.payment.dataaccess.entity;

import com.ticket.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentStatusTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "PAYMENT_REJECTED,        FAILED",
            "PAYMENT_RESULT_UNKNOWN,  UNKNOWN",
            "PG_UNAVAILABLE,          UNKNOWN",
            "DEFAULT_ERROR,           UNKNOWN",
    })
    @DisplayName("승인 거절이 확정된 경우만 FAILED, 승인됐을 가능성이 조금이라도 있으면 UNKNOWN")
    void ofConfirmFailure(ErrorType errorType, PaymentStatus expected) {
        assertThat(PaymentStatus.ofConfirmFailure(errorType)).isEqualTo(expected);
    }
}
