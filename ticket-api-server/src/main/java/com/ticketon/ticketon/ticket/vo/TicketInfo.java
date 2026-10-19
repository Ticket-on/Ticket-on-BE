package com.ticketon.ticketon.ticket.vo;

import com.ticketon.ticketon.ticket.dataaccess.entity.Ticket;
import com.ticketon.ticketon.ticket.dataaccess.entity.TicketStatus;

public record TicketInfo(
        Long id,
        Long ticketTypeId,
        String ticketTypeName,
        String eventTitle,
        Long memberId,
        TicketStatus ticketStatus,
        Integer price
) {
    public static TicketInfo from(Ticket ticket) {
        return new TicketInfo(
                ticket.getId(),
                ticket.getTicketTypeId(),
                ticket.getTicketType().getName(),
                ticket.getTicketType().getEventItem().getTitle(),
                ticket.getMemberId(),
                ticket.getTicketStatus(),
                ticket.getPrice()
        );
    }
}
