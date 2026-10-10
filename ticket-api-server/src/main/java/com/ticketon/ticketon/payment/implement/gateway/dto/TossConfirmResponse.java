package com.ticketon.ticketon.payment.implement.gateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ticketon.ticketon.payment.vo.PaymentApproval;
import com.ticketon.ticketon.payment.vo.PaymentInquiry;

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

    public PaymentInquiry toInquiry() {
        return new PaymentInquiry(
                orderId,
                paymentKey,
                totalAmount,
                status,
                requestedAt != null ? requestedAt.toLocalDateTime() : null,
                approvedAt != null ? approvedAt.toLocalDateTime() : null
        );
    }
}
