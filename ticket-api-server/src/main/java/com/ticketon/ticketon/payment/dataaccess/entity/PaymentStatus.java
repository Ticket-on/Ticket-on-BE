package com.ticketon.ticketon.payment.dataaccess.entity;

import com.ticket.support.exception.ErrorType;

public enum PaymentStatus {
    SUCCESS, PAID, FAILED, CANCELED, UNKNOWN;

    public static PaymentStatus ofConfirmFailure(ErrorType errorType) {
        if (errorType == ErrorType.PAYMENT_REJECTED || errorType == ErrorType.PG_CIRCUIT_OPEN) {
            return FAILED;
        }
        return UNKNOWN;
    }
}
