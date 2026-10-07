package com.ticketon.ticketon.payment.controller.response;

import com.ticketon.ticketon.payment.dataaccess.entity.PaymentStatus;
import com.ticketon.ticketon.payment.vo.PaymentInfo;

import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long ticketTypeId,
        Long memberId,
        String orderId,
        PaymentStatus status,
        Integer amount,
        LocalDateTime approvedAt
) {
    public static PaymentResponse from(PaymentInfo info) {
        return new PaymentResponse(
                info.id(),
                info.ticketTypeId(),
                info.memberId(),
                info.orderId(),
                info.status(),
                info.amount(),
                info.approvedAt()
        );
    }
}
