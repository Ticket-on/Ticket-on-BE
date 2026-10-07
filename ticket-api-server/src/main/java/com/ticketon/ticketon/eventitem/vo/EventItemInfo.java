package com.ticketon.ticketon.eventitem.vo;

import com.ticketon.ticketon.eventitem.dataaccess.entity.EventItem;
import com.ticketon.ticketon.ticket.vo.TicketTypeInfo;

import java.time.LocalDate;
import java.util.List;

public record EventItemInfo(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        List<TicketTypeInfo> ticketTypes
) {
    public static EventItemInfo from(EventItem eventItem) {
        return new EventItemInfo(
                eventItem.getId(),
                eventItem.getTitle(),
                eventItem.getStartDate(),
                eventItem.getEndDate(),
                eventItem.getTicketTypes().stream()
                        .map(TicketTypeInfo::from)
                        .toList()
        );
    }
}
