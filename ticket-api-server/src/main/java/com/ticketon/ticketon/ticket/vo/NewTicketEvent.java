package com.ticketon.ticketon.ticket.vo;

public record NewTicketEvent(Long ticketTypeId, Long memberId, String orderId, Integer price) {
}
