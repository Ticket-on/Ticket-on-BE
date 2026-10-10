package com.ticketon.ticketon.payment.implement.gateway;

import com.ticketon.ticketon.payment.vo.PaymentApproval;
import com.ticketon.ticketon.payment.vo.PaymentCancellation;
import com.ticketon.ticketon.payment.vo.PaymentConfirmation;
import com.ticketon.ticketon.payment.vo.PaymentInquiry;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Profile("loadtest")
@Component
public class FakePaymentGateway implements PaymentGateway {

    @Override
    public PaymentApproval requestPaymentConfirm(PaymentConfirmation confirmation) {
        LocalDateTime now = LocalDateTime.now();
        return new PaymentApproval(confirmation.paymentKey(), confirmation.orderId(), confirmation.amount(), now, now);
    }

    @Override
    public LocalDateTime requestPaymentCancel(PaymentCancellation cancellation) {
        return LocalDateTime.now();
    }

    @Override
    public PaymentInquiry inquire(String orderId) {
        return PaymentInquiry.notFound(orderId);
    }
}
