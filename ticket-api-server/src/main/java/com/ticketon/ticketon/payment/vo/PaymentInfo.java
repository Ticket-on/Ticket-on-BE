package com.ticketon.ticketon.payment.vo;

import com.ticketon.ticketon.payment.dataaccess.entity.Payment;
import com.ticketon.ticketon.payment.dataaccess.entity.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentInfo(
        Long id,
        Long ticketTypeId,
        Long memberId,
        String orderId,
        PaymentStatus status,
        Integer amount,
        LocalDateTime approvedAt
) {
    public static PaymentInfo from(Payment payment) {
        return new PaymentInfo(
                payment.getId(),
                payment.getTicketTypeId(),
                payment.getMemberId(),
                payment.getOrderId(),
                payment.getPaymentStatus(),
                payment.getAmount(),
                payment.getApprovedAt()
        );
    }
}
