package com.ticketon.ticketon.ticket.implement;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.ticket.dataaccess.entity.TicketType;
import com.ticketon.ticketon.ticket.dataaccess.repository.TicketTypeRepository;
import com.ticketon.ticketon.ticket.vo.TicketTypeInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TicketTypeReader {

    private final TicketTypeRepository ticketTypeRepository;

    public TicketType findById(Long ticketTypeId) {
        return ticketTypeRepository.findById(ticketTypeId)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_TICKET_TYPE));
    }

    public TicketTypeInfo findInfoById(Long ticketTypeId) {
        return TicketTypeInfo.from(findById(ticketTypeId));
    }

    public Map<Long, TicketType> findAllByIds(Collection<Long> ticketTypeIds) {
        return ticketTypeRepository.findAllById(ticketTypeIds)
                .stream()
                .collect(Collectors.toMap(TicketType::getId, Function.identity()));
    }

    public List<TicketTypeInfo> findAll() {
        return ticketTypeRepository.findAll()
                .stream()
                .map(TicketTypeInfo::from)
                .toList();
    }
}
