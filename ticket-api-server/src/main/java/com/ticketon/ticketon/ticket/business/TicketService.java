package com.ticketon.ticketon.ticket.business;

import com.ticketon.ticketon.common.utils.OrderIdGenerator;
import com.ticketon.ticketon.ticket.implement.PendingTicketManager;
import com.ticketon.ticketon.ticket.implement.TicketExpiryManager;
import com.ticketon.ticketon.ticket.implement.TicketManager;
import com.ticketon.ticketon.ticket.implement.TicketReader;
import com.ticketon.ticketon.ticket.implement.TicketTypeReader;
import com.ticketon.ticketon.ticket.implement.issue.TicketIssueStrategy;
import com.ticketon.ticketon.ticket.vo.ExpiredTicket;
import com.ticketon.ticketon.ticket.vo.NewTicketEvent;
import com.ticketon.ticketon.ticket.vo.TicketInfo;
import com.ticketon.ticketon.ticket.vo.TicketOrder;
import com.ticketon.ticketon.ticket.vo.TicketReservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketIssueStrategy ticketIssueStrategy;
    private final TicketManager ticketManager;
    private final TicketReader ticketReader;
    private final TicketTypeReader ticketTypeReader;
    private final TicketExpiryManager ticketExpiryManager;
    private final PendingTicketManager pendingTicketManager;

    public TicketOrder createOrder(Long memberId, Long ticketTypeId) {
        return TicketOrder.of(memberId, ticketTypeReader.findInfoById(ticketTypeId));
    }

    public TicketReservation reserve(TicketOrder order) {
        String orderId = OrderIdGenerator.nextId();
        TicketReservation reservation = ticketIssueStrategy.issue(order, orderId);
        log.info("[티켓 예매 : 재고 선점 완료]: orderId={} | ticketTypeId={} | memberId={}",
                orderId, order.ticketTypeId(), order.memberId());
        return reservation;
    }

    public void issueInBatch(List<NewTicketEvent> events) {
        int issuedCount = ticketManager.issueInBatch(events);
        log.info("[티켓 배치 발급 : 저장 완료]: requested={} | issued={}", events.size(), issuedCount);
    }

    public void expirePendingTickets() {
        LocalDateTime now = LocalDateTime.now();
        List<ExpiredTicket> unpaidTickets = ticketReader.findExpired(now)
                .stream()
                .filter(ticket -> !pendingTicketManager.isPaid(ticket.orderId()))
                .toList();

        if (!unpaidTickets.isEmpty()) {
            ticketExpiryManager.expire(unpaidTickets, now);
        }
        log.info("[티켓 만료 : 처리 완료]: expired={}", unpaidTickets.size());
    }

    public List<TicketInfo> getMyTickets(Long memberId) {
        return ticketReader.findInfosByMemberId(memberId);
    }

    public void cancelMyTicket(Long memberId, Long ticketId) {
        ticketManager.cancel(memberId, ticketId);
    }
}
