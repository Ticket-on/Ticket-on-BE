package com.ticketon.ticketon.domain.ticket.service.strategy;

import com.ticketon.ticketon.domain.member.entity.Member;
import com.ticketon.ticketon.domain.member.repository.MemberRepository;
import com.ticketon.ticketon.domain.ticket.entity.Ticket;
import com.ticketon.ticketon.domain.ticket.entity.TicketType;
import com.ticketon.ticketon.domain.ticket.entity.dto.TicketPurchaseRequest;
import com.ticketon.ticketon.domain.ticket.repository.TicketRepository;
import com.ticketon.ticketon.domain.ticket.repository.TicketTypeRepository;
import com.ticketon.ticketon.exception.custom.NotFoundDataException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("optimistic")
@Qualifier("optimistic")
@RequiredArgsConstructor
public class OptimisticLockTicketIssueService implements TicketIssueStrategy {

    private final TicketRepository ticketRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final MemberRepository memberRepository;

    @Transactional
    @Override
    public void purchaseTicket(TicketPurchaseRequest request, Long memberId) {
        Long ticketTypeId = request.getTicketTypeId();
        TicketType ticketType = ticketTypeRepository.findById(ticketTypeId)
                .orElseThrow(() -> new NotFoundDataException("티켓 타입 없음 (ticket_id=" + ticketTypeId + ")" ));

        Member member = memberRepository.getReferenceById(memberId);

        ticketType.increaseIssuedQuantity(); // @Version으로 낙관적 락 처리됨

        Ticket ticket = Ticket.createNormalTicket(ticketType, member);
        ticketRepository.save(ticket);
    }

}
