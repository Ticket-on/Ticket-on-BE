package com.ticketon.ticketon.domain.payment.dto;

import com.ticketon.ticketon.domain.payment.entity.Payment;
import com.ticketon.ticketon.domain.payment.entity.PaymentStatus;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Builder
public class PaymentConfirmRequest {
    private Long ticketTypeId;
    private String paymentKey;
    private Long memberId;
    private String orderId;
    private int amount;

    public TossConfirmRequest toTossConfirmRequest() {
        return new TossConfirmRequest(
                this.paymentKey,
                this.orderId,
                this.amount);
    }



}
