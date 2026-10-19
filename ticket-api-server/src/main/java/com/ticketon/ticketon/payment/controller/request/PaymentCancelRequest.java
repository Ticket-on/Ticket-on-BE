package com.ticketon.ticketon.payment.controller.request;

import com.ticketon.ticketon.payment.vo.PaymentCancellation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record PaymentCancelRequest(
        @NotBlank String paymentKey,
        @NotBlank String cancelReason,
        @Positive int cancelAmount
) {
    public PaymentCancellation toCancellation() {
        return new PaymentCancellation(paymentKey, cancelReason, cancelAmount);
    }
}
