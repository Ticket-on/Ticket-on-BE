package com.ticketon.ticketon.config.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Profile("!test")
@Slf4j
@Configuration
public class PaymentUnknownConsumerConfig {

    private static final String DLT_SUFFIX = "-dlt";
    private static final long RETRY_INTERVAL_MS = 10_000L;
    private static final long MAX_RETRIES = 1L;
    private static final int CONCURRENCY = 3;

    @Value("${kafka.consumer.payment-unknown-group.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.consumer.payment-unknown-group.group-id}")
    private String groupId;

    @Bean("paymentUnknownKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> paymentUnknownKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConcurrency(CONCURRENCY);
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, groupId,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
        )));
        factory.setCommonErrorHandler(paymentUnknownErrorHandler());
        return factory;
    }

    @Bean("paymentUnknownErrorHandler")
    public DefaultErrorHandler paymentUnknownErrorHandler() {
        DeadLetterPublishingRecoverer deadLetter = new DeadLetterPublishingRecoverer(paymentUnknownDltKafkaTemplate(),
                (record, e) -> new TopicPartition(record.topic() + DLT_SUFFIX, record.partition()));

        DefaultErrorHandler handler = new DefaultErrorHandler((record, e) -> {
            log.error("[결제 UNKNOWN 컨슈머 : DLT 전송]: partition={} | offset={} | error={} | 수동 확인 필요",
                    record.partition(), record.offset(), e.getMessage());
            deadLetter.accept(record, e);
        }, new FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRIES));
        handler.addNotRetryableExceptions(JsonProcessingException.class, DeserializationException.class);
        return handler;
    }

    @Bean("paymentUnknownDltKafkaTemplate")
    public KafkaTemplate<String, String> paymentUnknownDltKafkaTemplate() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class
        )));
    }
}
