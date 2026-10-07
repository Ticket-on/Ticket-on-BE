package com.ticketon.ticketon.eventitem.controller.response;

import com.ticketon.ticketon.eventitem.vo.EventItemInfo;

import java.time.LocalDate;
import java.util.List;

public record EventItemResponse(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        List<TicketTypeResponse> ticketTypes
) {
    public static EventItemResponse from(EventItemInfo info) {
        return new EventItemResponse(
                info.id(),
                info.title(),
                info.startDate(),
                info.endDate(),
                info.ticketTypes().stream()
                        .map(TicketTypeResponse::from)
                        .toList()
        );
    }
}
