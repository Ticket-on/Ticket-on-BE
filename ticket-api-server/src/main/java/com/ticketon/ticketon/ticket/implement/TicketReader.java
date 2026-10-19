package com.ticketon.ticketon.ticket.implement;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.ticket.dataaccess.entity.Ticket;
import com.ticketon.ticketon.ticket.dataaccess.repository.TicketRepository;
import com.ticketon.ticketon.ticket.vo.ExpiredTicket;
import com.ticketon.ticketon.ticket.vo.TicketInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TicketReader {

    private final TicketRepository ticketRepository;

    public Ticket findById(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_TICKET));
    }

    public Map<String, Ticket> findAllByOrderIds(Collection<String> orderIds) {
        return ticketRepository.findAllByOrderIdIn(orderIds)
                .stream()
                .collect(Collectors.toMap(Ticket::getOrderId, Function.identity()));
    }

    @Transactional(readOnly = true)
    public List<TicketInfo> findInfosByMemberId(Long memberId) {
        return ticketRepository.findByMember_Id(memberId)
                .stream()
                .map(TicketInfo::from)
                .toList();
    }

    public List<ExpiredTicket> findExpired(LocalDateTime now) {
        return ticketRepository.findExpiredTickets(now);
    }
}
