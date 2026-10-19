package com.ticketon.ticketon.ticket.consumer;

import com.ticketon.ticketon.ticket.business.TicketService;
import com.ticketon.ticketon.ticket.vo.NewTicketEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Profile("!test")
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketConsumer {

    private final TicketService ticketService;

    @KafkaListener(
            topics = "${kafka.topic-config.ticket.name}",
            groupId = "${kafka.consumer.ticket-group.group-id}",
            containerFactory = "ticketKafkaListenerContainerFactory")
    public void consumeNewTicket(List<NewTicketEvent> ticketEvents, Acknowledgment ack) {
        try {
            ticketService.issueInBatch(ticketEvents);
        } catch (Exception e) {
            log.warn("[티켓 컨슈머 : 배치 발급 실패]: size={} | error={}", ticketEvents.size(), e.getMessage());
        }
        ack.acknowledge();
    }
}
