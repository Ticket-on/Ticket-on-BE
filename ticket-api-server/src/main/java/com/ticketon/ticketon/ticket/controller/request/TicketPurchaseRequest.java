package com.ticketon.ticketon.ticket.controller.request;

import com.ticketon.ticketon.ticket.vo.TicketOrder;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

public record TicketPurchaseRequest(
        @NotNull Long ticketTypeId,
        @NotNull Long memberId,
        Integer amount,
        @Max(TicketOrder.QUANTITY) Integer quantity
) {
    public TicketOrder toOrder() {
        return new TicketOrder(ticketTypeId, memberId, amount);
    }
}
