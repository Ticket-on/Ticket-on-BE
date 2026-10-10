package com.ticketon.ticketon.payment.implement;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.payment.dataaccess.entity.Payment;
import com.ticketon.ticketon.payment.dataaccess.entity.PaymentStatus;
import com.ticketon.ticketon.payment.dataaccess.repository.PaymentRepository;
import com.ticketon.ticketon.payment.vo.PaymentInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public Map<String, Payment> findUnknownByOrderIds(Collection<String> orderIds) {
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        return paymentRepository.findAllByOrderIdInAndPaymentStatus(orderIds, PaymentStatus.UNKNOWN)
                .stream()
                .collect(Collectors.toMap(Payment::getOrderId, Function.identity()));
    }

    public Set<String> findUnknownOrderIds(Collection<String> orderIds) {
        return findUnknownByOrderIds(orderIds).keySet();
    }
}
