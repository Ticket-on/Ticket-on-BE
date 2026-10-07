package com.ticketon.ticketon.payment.controller.response;

import com.ticketon.ticketon.payment.vo.PaymentMessage;

import java.time.LocalDateTime;

public record PaymentConfirmResponse(
        String paymentKey,
        Long memberId,
        String orderId,
        Integer amount,
        LocalDateTime requestedAt,
        LocalDateTime approvedAt
) {
    public static PaymentConfirmResponse from(PaymentMessage message) {
        return new PaymentConfirmResponse(
                message.paymentKey(),
                message.memberId(),
                message.orderId(),
                message.amount(),
                message.requestedAt(),
                message.approvedAt()
        );
    }
}
