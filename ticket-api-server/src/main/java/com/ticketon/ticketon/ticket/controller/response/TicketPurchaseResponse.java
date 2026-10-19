package com.ticketon.ticketon.ticket.controller.response;

import com.ticketon.ticketon.ticket.vo.TicketReservation;

public record TicketPurchaseResponse(String orderId) {

    public static TicketPurchaseResponse from(TicketReservation reservation) {
        return new TicketPurchaseResponse(reservation.orderId());
    }
}
