package com.ticketon.ticketon.payment.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketon.ticketon.payment.business.PaymentService;
import com.ticketon.ticketon.payment.vo.PaymentMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Profile("!test")
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConsumer {

    private static final String DEBEZIUM_PAYLOAD_FIELD = "payload";

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topic-config.payment.name}",
            groupId = "${kafka.consumer.payment-group.group-id}",
            containerFactory = "paymentKafkaListenerContainerFactory")
    public void consumePayment(List<String> messages, Acknowledgment ack) {
        try {
            paymentService.complete(parse(messages));
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[결제 컨슈머 : 티켓 확정 실패]: size={} | error={}", messages.size(), e.getMessage(), e);
            throw new IllegalStateException("티켓 확정 실패로 인한 재시도", e);
        }
    }

    private List<PaymentMessage> parse(List<String> messages) throws JsonProcessingException {
        List<PaymentMessage> paymentMessages = new ArrayList<>();
        for (String message : messages) {
            paymentMessages.add(objectMapper.treeToValue(
                    objectMapper.readTree(message).get(DEBEZIUM_PAYLOAD_FIELD), PaymentMessage.class));
        }
        return paymentMessages;
    }
}
