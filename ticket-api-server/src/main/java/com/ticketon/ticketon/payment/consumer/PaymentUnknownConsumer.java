package com.ticketon.ticketon.payment.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketon.ticketon.payment.business.PaymentService;
import com.ticketon.ticketon.payment.vo.PaymentUnknownEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Profile("!test")
@Component
@RequiredArgsConstructor
public class PaymentUnknownConsumer {

    private static final String DEBEZIUM_PAYLOAD_FIELD = "payload";

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topic-config.payment-unknown.name}",
            groupId = "${kafka.consumer.payment-unknown-group.group-id}",
            containerFactory = "paymentUnknownKafkaListenerContainerFactory")
    public void consume(String message) throws JsonProcessingException {
        paymentService.resolveUnknown(parse(message));
    }

    private PaymentUnknownEvent parse(String message) throws JsonProcessingException {
        return objectMapper.treeToValue(objectMapper.readTree(message).get(DEBEZIUM_PAYLOAD_FIELD), PaymentUnknownEvent.class);
    }
}
