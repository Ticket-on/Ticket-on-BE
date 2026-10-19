package com.ticketon.ticketon.eventitem.business;

import com.ticketon.ticketon.eventitem.implement.EventItemReader;
import com.ticketon.ticketon.eventitem.vo.EventItemInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventItemService {

    private final EventItemReader eventItemReader;

    public List<EventItemInfo> getEventItems() {
        return eventItemReader.findAllOrderByStartDate();
    }

    public EventItemInfo getEventItem(Long eventItemId) {
        return eventItemReader.findInfoById(eventItemId);
    }

    public String getTitleByTicketTypeId(Long ticketTypeId) {
        return eventItemReader.findTitleByTicketTypeId(ticketTypeId);
    }
}
