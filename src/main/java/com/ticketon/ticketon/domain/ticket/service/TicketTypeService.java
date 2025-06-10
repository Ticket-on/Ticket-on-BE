package com.ticketon.ticketon.domain.ticket.service;

import com.ticketon.ticketon.domain.eventitem.entity.EventItem;
import com.ticketon.ticketon.domain.eventitem.repository.EventItemRepository;
import com.ticketon.ticketon.domain.ticket.entity.dto.TicketTypeCreateRequest;
import com.ticketon.ticketon.domain.ticket.repository.TicketTypeRepository;
import com.ticketon.ticketon.exception.custom.NotFoundDataException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Transactional
@Service
@RequiredArgsConstructor
public class TicketTypeService {
    private final TicketTypeRepository ticketTypeRepository;
    private final EventItemRepository eventItemRepository;

    public void saveTicketType(TicketTypeCreateRequest request, Long eventId) {
        EventItem event = eventItemRepository.findById(eventId).orElseThrow(() -> new NotFoundDataException("해당 공연(이벤트)을 찾을 수 없습니다 (id=" + eventId + ")"));
        // dto -> entity 변환 후 저장
        ticketTypeRepository.save(request.toEntityWithEventItem(event));
    }
}
