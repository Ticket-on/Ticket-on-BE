package com.ticketon.ticketon.payment.vo;

public record PaymentUnknownEvent(String orderId, String paymentKey, Long memberId, Integer amount) {

    public static PaymentUnknownEvent from(PaymentConfirmation confirmation) {
        return new PaymentUnknownEvent(
                confirmation.orderId(), confirmation.paymentKey(), confirmation.memberId(), confirmation.amount());
    }
}
