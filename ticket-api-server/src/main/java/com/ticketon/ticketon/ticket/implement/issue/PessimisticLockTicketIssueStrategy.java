package com.ticketon.ticketon.ticket.implement.issue;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.member.dataaccess.entity.Member;
import com.ticketon.ticketon.member.implement.MemberReader;
import com.ticketon.ticketon.ticket.dataaccess.entity.Ticket;
import com.ticketon.ticketon.ticket.dataaccess.entity.TicketType;
import com.ticketon.ticketon.ticket.dataaccess.repository.TicketTypeRepository;
import com.ticketon.ticketon.ticket.implement.TicketWriter;
import com.ticketon.ticketon.ticket.vo.TicketOrder;
import com.ticketon.ticketon.ticket.vo.TicketReservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PessimisticLockTicketIssueStrategy implements TicketIssueStrategy {

    private final TicketTypeRepository ticketTypeRepository;
    private final TicketWriter ticketWriter;
    private final MemberReader memberReader;

    @Override
    @Transactional
    public TicketReservation issue(TicketOrder order, String orderId) {
        TicketType ticketType = ticketTypeRepository.findByIdForUpdate(order.ticketTypeId())
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_TICKET_TYPE));
        Member member = memberReader.findById(order.memberId());

        ticketType.increaseIssuedQuantity();
        Ticket ticket = ticketWriter.save(Ticket.createPending(ticketType, member, orderId));
        return TicketReservation.from(ticket);
    }
}
