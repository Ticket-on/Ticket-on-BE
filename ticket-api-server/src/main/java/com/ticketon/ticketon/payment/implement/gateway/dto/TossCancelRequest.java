package com.ticketon.ticketon.payment.implement.gateway.dto;

import com.ticketon.ticketon.payment.vo.PaymentCancellation;

public record TossCancelRequest(String cancelReason, int cancelAmount) {

    public static TossCancelRequest from(PaymentCancellation cancellation) {
        return new TossCancelRequest(cancellation.cancelReason(), cancellation.cancelAmount());
    }
}
