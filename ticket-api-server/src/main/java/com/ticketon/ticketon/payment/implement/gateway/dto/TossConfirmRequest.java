package com.ticketon.ticketon.payment.implement.gateway.dto;

import com.ticketon.ticketon.payment.vo.PaymentConfirmation;

public record TossConfirmRequest(String paymentKey, String orderId, int amount) {

    public static TossConfirmRequest from(PaymentConfirmation confirmation) {
        return new TossConfirmRequest(confirmation.paymentKey(), confirmation.orderId(), confirmation.amount());
    }
}
