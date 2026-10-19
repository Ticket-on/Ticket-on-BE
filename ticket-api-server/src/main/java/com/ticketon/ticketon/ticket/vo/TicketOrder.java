package com.ticketon.ticketon.ticket.vo;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;

public record TicketOrder(Long ticketTypeId, Long memberId, Integer amount) {

    public static final int QUANTITY = 1;

    public TicketOrder {
        if (ticketTypeId == null || memberId == null) {
            throw new AppException(ErrorType.INVALID_REQUEST);
        }
    }

    public static TicketOrder of(Long memberId, TicketTypeInfo ticketType) {
        return new TicketOrder(ticketType.id(), memberId, ticketType.price());
    }

    public NewTicketEvent toEvent(String orderId) {
        return new NewTicketEvent(ticketTypeId, memberId, orderId, amount);
    }

    public PendingTicket toPendingTicket(String orderId) {
        return PendingTicket.of(orderId, ticketTypeId, memberId);
    }
}
