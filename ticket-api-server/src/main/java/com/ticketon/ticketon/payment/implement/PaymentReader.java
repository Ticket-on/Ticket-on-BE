package com.ticketon.ticketon.payment.implement;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.payment.dataaccess.entity.Payment;
import com.ticketon.ticketon.payment.dataaccess.repository.PaymentRepository;
import com.ticketon.ticketon.payment.vo.PaymentInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentReader {

    private final PaymentRepository paymentRepository;

    public Payment findByPaymentKey(String paymentKey) {
        return paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_PAYMENT));
    }

    public PaymentInfo findInfoByPaymentKey(String paymentKey) {
        return PaymentInfo.from(findByPaymentKey(paymentKey));
    }

    public PaymentInfo findInfoByTicketId(Long ticketId) {
        return paymentRepository.findByTicketId(ticketId)
                .map(PaymentInfo::from)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_PAYMENT));
    }
}
