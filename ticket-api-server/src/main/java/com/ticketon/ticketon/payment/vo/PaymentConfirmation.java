package com.ticketon.ticketon.payment.vo;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;

public record PaymentConfirmation(String paymentKey, Long memberId, String orderId, Integer amount) {

    public PaymentConfirmation {
        if (paymentKey == null || paymentKey.isBlank() || orderId == null || orderId.isBlank() || amount == null) {
            throw new AppException(ErrorType.INVALID_REQUEST);
        }
    }
}
