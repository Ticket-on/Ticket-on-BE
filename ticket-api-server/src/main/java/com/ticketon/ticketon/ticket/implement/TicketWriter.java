package com.ticketon.ticketon.ticket.implement;

import com.ticketon.ticketon.ticket.dataaccess.entity.Ticket;
import com.ticketon.ticketon.ticket.dataaccess.repository.TicketJdbcRepository;
import com.ticketon.ticketon.ticket.dataaccess.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TicketWriter {

    private final TicketRepository ticketRepository;
    private final TicketJdbcRepository ticketJdbcRepository;

    public Ticket save(Ticket ticket) {
        return ticketRepository.save(ticket);
    }

    public void saveAllInBatch(List<Ticket> tickets) {
        ticketJdbcRepository.batchInsert(tickets);
    }

    public void cancelExpired(List<Long> ticketIds, LocalDateTime now) {
        ticketRepository.bulkUpdateStatusToExpiredByIds(ticketIds, now);
    }
}
