package com.ticketon.ticketon.ticket.vo;

import com.ticketon.ticketon.ticket.dataaccess.entity.Ticket;
import com.ticketon.ticketon.ticket.dataaccess.entity.TicketStatus;

import java.time.LocalDateTime;

public record TicketReservation(
        Long ticketId,
        Long ticketTypeId,
        Long memberId,
        TicketStatus ticketStatus,
        Integer price,
        String orderId,
        LocalDateTime expiredAt
) {
    public static TicketReservation from(Ticket ticket) {
        return new TicketReservation(
                ticket.getId(),
                ticket.getTicketTypeId(),
                ticket.getMemberId(),
                ticket.getTicketStatus(),
                ticket.getPrice(),
                ticket.getOrderId(),
                ticket.getExpiredAt()
        );
    }

    public static TicketReservation pending(TicketOrder order, String orderId) {
        return new TicketReservation(
                null,
                order.ticketTypeId(),
                order.memberId(),
                TicketStatus.PENDING,
                order.amount(),
                orderId,
                null
        );
    }
}
