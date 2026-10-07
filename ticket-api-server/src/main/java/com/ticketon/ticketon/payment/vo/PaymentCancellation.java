package com.ticketon.ticketon.payment.vo;

public record PaymentCancellation(String paymentKey, String cancelReason, int cancelAmount) {
}
