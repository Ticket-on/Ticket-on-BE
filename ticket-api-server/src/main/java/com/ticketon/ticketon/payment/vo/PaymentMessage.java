package com.ticketon.ticketon.payment.vo;

import java.time.LocalDateTime;

public record PaymentMessage(
        String paymentKey,
        Long memberId,
        String orderId,
        Integer amount,
        LocalDateTime requestedAt,
        LocalDateTime approvedAt
) {
    public static PaymentMessage of(PaymentConfirmation confirmation, PaymentApproval approval) {
        return new PaymentMessage(
                approval.paymentKey(),
                confirmation.memberId(),
                confirmation.orderId(),
                approval.amount(),
                approval.requestedAt(),
                approval.approvedAt()
        );
    }
}
