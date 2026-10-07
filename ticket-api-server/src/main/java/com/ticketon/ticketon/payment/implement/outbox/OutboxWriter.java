package com.ticketon.ticketon.payment.implement.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.payment.dataaccess.entity.OutboxMessage;
import com.ticketon.ticketon.payment.dataaccess.repository.OutboxRepository;
import com.ticketon.ticketon.payment.vo.PaymentMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxWriter {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void savePaymentConfirmed(PaymentMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            outboxRepository.save(OutboxMessage.paymentConfirmed(payload));
            log.info("[결제 아웃박스 : 저장 완료]: orderId={}", message.orderId());
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorType.OUTBOX_SERIALIZATION_FAILED, e);
        }
    }
}
