package com.ticketon.ticketon.payment.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketon.ticketon.payment.business.PaymentService;
import com.ticketon.ticketon.payment.vo.PaymentUnknownEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Profile("!test")
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentUnknownConsumer {

    private static final String DEBEZIUM_PAYLOAD_FIELD = "payload";

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 5_000, multiplier = 6, maxDelay = 120_000),
            kafkaTemplate = "paymentUnknownRetryKafkaTemplate",
            listenerContainerFactory = "paymentUnknownKafkaListenerContainerFactory",
            autoCreateTopics = "false",
            dltTopicSuffix = "-dlt",
            exclude = {JsonProcessingException.class, DeserializationException.class})
    @KafkaListener(
            topics = "${kafka.topic-config.payment-unknown.name}",
            groupId = "${kafka.consumer.payment-unknown-group.group-id}",
            containerFactory = "paymentUnknownKafkaListenerContainerFactory")
    public void consume(String message) throws JsonProcessingException {
        paymentService.resolveUnknown(parse(message));
    }

    @DltHandler
    public void handleDlt(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("[결제 UNKNOWN 컨슈머 : DLT 도착]: topic={} | message={} | 수동 확인 필요", topic, message);
    }

    private PaymentUnknownEvent parse(String message) throws JsonProcessingException {
        return objectMapper.treeToValue(objectMapper.readTree(message).get(DEBEZIUM_PAYLOAD_FIELD), PaymentUnknownEvent.class);
    }
}
