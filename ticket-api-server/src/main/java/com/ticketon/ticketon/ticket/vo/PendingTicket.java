package com.ticketon.ticketon.ticket.vo;

import com.ticketon.ticketon.ticket.dataaccess.entity.TicketStatus;

public record PendingTicket(String orderId, Long ticketTypeId, Long memberId, TicketStatus status) {

    public static PendingTicket of(String orderId, Long ticketTypeId, Long memberId) {
        return new PendingTicket(orderId, ticketTypeId, memberId, TicketStatus.PENDING);
    }
}
