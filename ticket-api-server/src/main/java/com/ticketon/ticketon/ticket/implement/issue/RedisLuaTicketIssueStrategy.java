package com.ticketon.ticketon.ticket.implement.issue;

import com.ticketon.ticketon.ticket.implement.PendingTicketManager;
import com.ticketon.ticketon.ticket.implement.TicketEventProducer;
import com.ticketon.ticketon.ticket.implement.stock.TicketStockManager;
import com.ticketon.ticketon.ticket.vo.TicketOrder;
import com.ticketon.ticketon.ticket.vo.TicketReservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class RedisLuaTicketIssueStrategy implements TicketIssueStrategy {

    private final TicketStockManager ticketStockManager;
    private final PendingTicketManager pendingTicketManager;
    private final TicketEventProducer ticketEventProducer;

    @Override
    public TicketReservation issue(TicketOrder order, String orderId) {
        ticketStockManager.decrease(order.ticketTypeId());
        pendingTicketManager.save(order.toPendingTicket(orderId));

        try {
            ticketEventProducer.sendNewTicket(order.toEvent(orderId));
        } catch (Exception e) {
            log.error("[티켓 발급 : Kafka 발행 실패]: orderId={} | 재고 복구 및 대기 티켓 삭제", orderId, e);
            ticketStockManager.restore(order.ticketTypeId());
            pendingTicketManager.delete(orderId);
            throw e;
        }

        return TicketReservation.pending(order, orderId);
    }
}
