package com.ticketon.ticketon.payment.vo;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

public record PaymentInquiry(
        String orderId,
        String paymentKey,
        Integer totalAmount,
        String status,
        LocalDateTime requestedAt,
        LocalDateTime approvedAt
) {
    private static final String NOT_FOUND = "NOT_FOUND";
    private static final String DONE = "DONE";
    private static final Set<String> FAILED_STATUSES = Set.of("ABORTED", "EXPIRED", "READY", NOT_FOUND);
    private static final Set<String> CANCELED_STATUSES = Set.of("CANCELED", "PARTIAL_CANCELED");

    public static PaymentInquiry notFound(String orderId) {
        return new PaymentInquiry(orderId, null, null, NOT_FOUND, null, null);
    }

    public UnknownResolution resolve(Integer expectedAmount) {
        if (DONE.equals(status)) {
            return Objects.equals(totalAmount, expectedAmount) ? UnknownResolution.PAID : UnknownResolution.AMOUNT_MISMATCH;
        }
        if (FAILED_STATUSES.contains(status)) {
            return UnknownResolution.FAILED;
        }
        if (CANCELED_STATUSES.contains(status)) {
            return UnknownResolution.CANCELED;
        }
        return UnknownResolution.PENDING;
    }

    public PaymentMessage toPaymentMessage(Long memberId) {
        return new PaymentMessage(paymentKey, memberId, orderId, totalAmount, requestedAt, approvedAt);
    }
}
