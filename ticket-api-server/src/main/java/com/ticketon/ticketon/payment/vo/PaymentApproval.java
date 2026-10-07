package com.ticketon.ticketon.payment.vo;

import java.time.LocalDateTime;

public record PaymentApproval(
        String paymentKey,
        String orderId,
        Integer amount,
        LocalDateTime requestedAt,
        LocalDateTime approvedAt
) {
}
