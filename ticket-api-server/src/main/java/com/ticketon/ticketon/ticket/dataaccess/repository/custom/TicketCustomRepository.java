package com.ticketon.ticketon.ticket.dataaccess.repository.custom;

import com.ticketon.ticketon.ticket.vo.ExpiredTicket;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketCustomRepository {
    List<ExpiredTicket> findExpiredTickets(LocalDateTime now);
    void bulkUpdateStatusToExpiredByIds(List<Long> ticketIds, LocalDateTime now);
}
