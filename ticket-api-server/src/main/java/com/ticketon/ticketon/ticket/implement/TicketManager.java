package com.ticketon.ticketon.ticket.implement;

import com.ticketon.ticketon.member.dataaccess.entity.Member;
import com.ticketon.ticketon.member.implement.MemberReader;
import com.ticketon.ticketon.ticket.dataaccess.entity.Ticket;
import com.ticketon.ticketon.ticket.dataaccess.entity.TicketType;
import com.ticketon.ticketon.ticket.vo.NewTicketEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketManager {

    private final TicketReader ticketReader;
    private final TicketWriter ticketWriter;
    private final TicketTypeReader ticketTypeReader;
    private final TicketValidator ticketValidator;
    private final MemberReader memberReader;

    @Transactional
    public int issueInBatch(List<NewTicketEvent> events) {
        Map<Long, TicketType> ticketTypes = ticketTypeReader.findAllByIds(
                events.stream().map(NewTicketEvent::ticketTypeId).toList());
        Map<Long, Member> members = memberReader.findAllByIds(
                events.stream().map(NewTicketEvent::memberId).toList());

        List<Ticket> tickets = new ArrayList<>();
        for (NewTicketEvent event : events) {
            TicketType ticketType = ticketTypes.get(event.ticketTypeId());
            Member member = members.get(event.memberId());

            if (ticketType == null) {
                log.error("[티켓 배치 발급 : 티켓 종류 없음]: ticketTypeId={} | orderId={}", event.ticketTypeId(), event.orderId());
                continue;
            }
            if (member == null) {
                log.error("[티켓 배치 발급 : 회원 없음]: memberId={} | orderId={}", event.memberId(), event.orderId());
                continue;
            }
            tickets.add(Ticket.createPending(ticketType, member, event.orderId()));
        }

        if (!tickets.isEmpty()) {
            ticketWriter.saveAllInBatch(tickets);
        }
        return tickets.size();
    }

    @Transactional
    public void cancel(Long memberId, Long ticketId) {
        Ticket ticket = ticketReader.findById(ticketId);
        ticketValidator.validateOwner(ticket, memberId);
        ticket.cancel();
        ticket.getTicketType().decreaseIssuedQuantity();
    }
}
