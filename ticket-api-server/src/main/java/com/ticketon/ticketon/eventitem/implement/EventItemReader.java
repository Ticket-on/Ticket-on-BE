package com.ticketon.ticketon.eventitem.implement;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.eventitem.dataaccess.entity.EventItem;
import com.ticketon.ticketon.eventitem.dataaccess.repository.EventItemRepository;
import com.ticketon.ticketon.eventitem.vo.EventItemInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventItemReader {

    private final EventItemRepository eventItemRepository;

    public List<EventItemInfo> findAllOrderByStartDate() {
        return eventItemRepository.findAllByOrderByStartDateAsc()
                .stream()
                .map(EventItemInfo::from)
                .toList();
    }

    public EventItemInfo findInfoById(Long eventItemId) {
        return EventItemInfo.from(findById(eventItemId));
    }

    public String findTitleByTicketTypeId(Long ticketTypeId) {
        return eventItemRepository.findByTicketTypes_id(ticketTypeId)
                .map(EventItem::getTitle)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_EVENT_ITEM));
    }

    private EventItem findById(Long eventItemId) {
        return eventItemRepository.findById(eventItemId)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_EVENT_ITEM));
    }
}
