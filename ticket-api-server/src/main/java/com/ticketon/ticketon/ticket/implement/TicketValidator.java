package com.ticketon.ticketon.ticket.implement;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.ticket.dataaccess.entity.Ticket;
import org.springframework.stereotype.Component;

@Component
public class TicketValidator {

    public void validateOwner(Ticket ticket, Long memberId) {
        if (!ticket.isOwnedBy(memberId)) {
            throw new AppException(ErrorType.NOT_TICKET_OWNER);
        }
    }
}
