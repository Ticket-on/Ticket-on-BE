package com.ticketon.ticketon.eventitem.controller.response;

import com.ticketon.ticketon.ticket.dataaccess.entity.TicketTypeStatus;
import com.ticketon.ticketon.ticket.vo.TicketTypeInfo;

public record TicketTypeResponse(
        Long id,
        String name,
        String description,
        Long maxIssueQuantity,
        Long currentIssuedCount,
        Integer price,
        TicketTypeStatus ticketTypeStatus
) {
    public static TicketTypeResponse from(TicketTypeInfo info) {
        return new TicketTypeResponse(
                info.id(),
                info.name(),
                info.description(),
                info.maxQuantity(),
                info.issuedQuantity(),
                info.price(),
                info.status()
        );
    }
}
