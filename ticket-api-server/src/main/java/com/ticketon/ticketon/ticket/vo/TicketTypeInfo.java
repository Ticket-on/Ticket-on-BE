package com.ticketon.ticketon.ticket.vo;

import com.ticketon.ticketon.ticket.dataaccess.entity.TicketType;
import com.ticketon.ticketon.ticket.dataaccess.entity.TicketTypeStatus;

public record TicketTypeInfo(
        Long id,
        String name,
        String description,
        Long maxQuantity,
        Long issuedQuantity,
        Integer price,
        TicketTypeStatus status
) {
    public static TicketTypeInfo from(TicketType ticketType) {
        return new TicketTypeInfo(
                ticketType.getId(),
                ticketType.getName(),
                ticketType.getDescription(),
                ticketType.getMaxQuantity(),
                ticketType.getIssuedQuantity(),
                ticketType.getPrice(),
                ticketType.getStatus()
        );
    }
}
