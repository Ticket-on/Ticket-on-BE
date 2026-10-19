package com.ticketon.ticketon.ticket.implement;

import com.ticketon.ticketon.ticket.vo.NewTicketEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic-config.ticket.name}")
    private String topic;

    public void sendNewTicket(NewTicketEvent event) {
        kafkaTemplate.send(topic, event);
    }
}
