package com.ticketon.ticketon.payment.implement.gateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ticketon.ticketon.payment.vo.PaymentApproval;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossConfirmResponse(
        String orderId,
        Integer totalAmount,
        String paymentKey,
        OffsetDateTime requestedAt,
        OffsetDateTime approvedAt,
        String status
) {
    public PaymentApproval toApproval() {
        return new PaymentApproval(
                paymentKey,
                orderId,
                totalAmount,
                requestedAt.toLocalDateTime(),
                approvedAt.toLocalDateTime()
        );
    }
}
