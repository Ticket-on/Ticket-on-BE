package com.ticketon.ticketon.payment.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentInquiryTest {

    private static final int AMOUNT = 10000;

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "ABORTED,             FAILED",
            "EXPIRED,             FAILED",
            "CANCELED,            CANCELED",
            "PARTIAL_CANCELED,    CANCELED",
            "IN_PROGRESS,         PENDING",
            "READY,               PENDING",
            "WAITING_FOR_DEPOSIT, PENDING",
            "SOME_NEW_STATUS,     PENDING",
    })
    @DisplayName("토스 결제 상태로 UNKNOWN 확정 결과를 정한다")
    void resolveByStatus(String status, UnknownResolution expected) {
        assertThat(inquiry(status, AMOUNT).resolve(AMOUNT)).isEqualTo(expected);
    }

    @Test
    @DisplayName("DONE 이고 금액이 같으면 PAID")
    void doneWithSameAmount() {
        assertThat(inquiry("DONE", AMOUNT).resolve(AMOUNT)).isEqualTo(UnknownResolution.PAID);
    }

    @Test
    @DisplayName("DONE 인데 금액이 다르면 AMOUNT_MISMATCH")
    void doneWithDifferentAmount() {
        assertThat(inquiry("DONE", 9999).resolve(AMOUNT)).isEqualTo(UnknownResolution.AMOUNT_MISMATCH);
    }

    @Test
    @DisplayName("토스에 결제가 없으면 PENDING")
    void notFound() {
        assertThat(PaymentInquiry.notFound("order-1").resolve(AMOUNT)).isEqualTo(UnknownResolution.PENDING);
    }

    private PaymentInquiry inquiry(String status, int totalAmount) {
        LocalDateTime now = LocalDateTime.now();
        return new PaymentInquiry("order-1", "pk", totalAmount, status, now, now);
    }
}
