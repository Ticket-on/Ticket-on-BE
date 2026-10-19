package com.ticketon.ticketon.ticket.implement;

import com.ticketon.ticketon.ticket.implement.stock.TicketStockManager;
import com.ticketon.ticketon.ticket.vo.ExpiredTicket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketExpiryManager {

    private final TicketWriter ticketWriter;
    private final TicketStockManager ticketStockManager;

    @Transactional
    public void expire(List<ExpiredTicket> expiredTickets, LocalDateTime now) {
        List<Long> ticketIds = expiredTickets.stream()
                .map(ExpiredTicket::ticketId)
                .toList();
        ticketWriter.cancelExpired(ticketIds, now);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                restoreStock(expiredTickets);
            }
        });
    }

    private void restoreStock(List<ExpiredTicket> expiredTickets) {
        for (ExpiredTicket ticket : expiredTickets) {
            try {
                ticketStockManager.restore(ticket.ticketTypeId());
                log.info("[티켓 만료 : 재고 복구 완료]: ticketId={} | ticketTypeId={}",
                        ticket.ticketId(), ticket.ticketTypeId());
            } catch (Exception e) {
                log.error("[티켓 만료 : 재고 복구 실패]: ticketId={} | ticketTypeId={} | 수동 확인 필요",
                        ticket.ticketId(), ticket.ticketTypeId(), e);
            }
        }
    }
}
