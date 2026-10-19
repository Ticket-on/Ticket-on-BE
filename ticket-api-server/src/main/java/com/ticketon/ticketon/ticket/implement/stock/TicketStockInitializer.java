package com.ticketon.ticketon.ticket.implement.stock;

import com.ticketon.ticketon.ticket.implement.TicketTypeReader;
import com.ticketon.ticketon.ticket.vo.TicketTypeInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketStockInitializer implements ApplicationRunner {

    private final TicketTypeReader ticketTypeReader;
    private final TicketStockManager ticketStockManager;

    @Override
    public void run(ApplicationArguments args) {
        for (TicketTypeInfo ticketType : ticketTypeReader.findAll()) {
            ticketStockManager.initialize(ticketType.id(), ticketType.maxQuantity());
        }
    }
}
