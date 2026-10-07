package com.ticketon.ticketon.ticket.implement.issue;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.member.dataaccess.entity.Member;
import com.ticketon.ticketon.member.implement.MemberReader;
import com.ticketon.ticketon.ticket.dataaccess.entity.Ticket;
import com.ticketon.ticketon.ticket.dataaccess.entity.TicketType;
import com.ticketon.ticketon.ticket.dataaccess.repository.TicketTypeRepository;
import com.ticketon.ticketon.ticket.implement.TicketTypeReader;
import com.ticketon.ticketon.ticket.implement.TicketWriter;
import com.ticketon.ticketon.ticket.vo.TicketOrder;
import com.ticketon.ticketon.ticket.vo.TicketReservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AtomicDbTicketIssueStrategy implements TicketIssueStrategy {

    private final TicketTypeRepository ticketTypeRepository;
    private final TicketTypeReader ticketTypeReader;
    private final TicketWriter ticketWriter;
    private final MemberReader memberReader;

    @Override
    @Transactional
    public TicketReservation issue(TicketOrder order, String orderId) {
        TicketType ticketType = ticketTypeReader.findById(order.ticketTypeId());
        Member member = memberReader.findById(order.memberId());

        long updatedRows = ticketTypeRepository.increaseTicketAtomically(order.ticketTypeId());
        if (updatedRows == 0) {
            throw new AppException(ErrorType.TICKET_SOLD_OUT);
        }

        Ticket ticket = ticketWriter.save(Ticket.createPending(ticketType, member, orderId));
        return TicketReservation.from(ticket);
    }
}
