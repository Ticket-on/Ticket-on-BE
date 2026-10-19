package com.ticketon.ticketon.ticket.scheduler;

import com.ticketon.ticketon.ticket.business.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketExpiryScheduler {

    private final TicketService ticketService;

//    @Scheduled(fixedRate = 60000)
    public void expirePendingTickets() {
        ticketService.expirePendingTickets();
    }
}
