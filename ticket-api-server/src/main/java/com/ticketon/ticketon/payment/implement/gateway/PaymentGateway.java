package com.ticketon.ticketon.payment.implement.gateway;

import com.ticketon.ticketon.payment.vo.PaymentApproval;
import com.ticketon.ticketon.payment.vo.PaymentCancellation;
import com.ticketon.ticketon.payment.vo.PaymentConfirmation;
import com.ticketon.ticketon.payment.vo.PaymentInquiry;

import java.time.LocalDateTime;

public interface PaymentGateway {

    PaymentApproval requestPaymentConfirm(PaymentConfirmation confirmation);

    LocalDateTime requestPaymentCancel(PaymentCancellation cancellation);

    PaymentInquiry inquire(String orderId);
}
