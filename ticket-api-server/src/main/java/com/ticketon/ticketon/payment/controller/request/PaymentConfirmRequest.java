package com.ticketon.ticketon.payment.controller.request;

import com.ticketon.ticketon.payment.vo.PaymentConfirmation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentConfirmRequest(
        @NotBlank String paymentKey,
        @NotNull Long memberId,
        @NotBlank String orderId,
        @NotNull Integer amount
) {
    public PaymentConfirmation toConfirmation() {
        return new PaymentConfirmation(paymentKey, memberId, orderId, amount);
    }
}
