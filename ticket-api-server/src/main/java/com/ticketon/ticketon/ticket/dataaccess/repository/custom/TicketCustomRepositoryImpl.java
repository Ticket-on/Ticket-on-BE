package com.ticketon.ticketon.ticket.dataaccess.repository.custom;

import com.querydsl.core.types.Projections;
import com.ticketon.ticketon.support.repository.QuerydslRepositorySupport;
import com.ticketon.ticketon.ticket.dataaccess.entity.Ticket;
import com.ticketon.ticketon.ticket.dataaccess.entity.TicketStatus;
import com.ticketon.ticketon.ticket.vo.ExpiredTicket;

import java.time.LocalDateTime;
import java.util.List;

import static com.ticketon.ticketon.ticket.dataaccess.entity.QTicket.ticket;

public class TicketCustomRepositoryImpl extends QuerydslRepositorySupport implements TicketCustomRepository {

    protected TicketCustomRepositoryImpl() {
        super(Ticket.class);
    }

    @Override
    public List<ExpiredTicket> findExpiredTickets(LocalDateTime now) {
        return select(Projections.constructor(ExpiredTicket.class,
                ticket.id,
                ticket.ticketType.id,
                ticket.orderId
        ))
                .from(ticket)
                .where(ticket.ticketStatus.eq(TicketStatus.PENDING),
                        ticket.expiredAt.lt(now))
                .fetch();
    }

    @Override
    public void bulkUpdateStatusToExpiredByIds(List<Long> ticketIds, LocalDateTime now) {
        update(ticket)
                .set(ticket.ticketStatus, TicketStatus.CANCELLED)
                .where(ticket.id.in(ticketIds),
                        ticket.ticketStatus.eq(TicketStatus.PENDING),
                        ticket.expiredAt.before(now)
                )
                .execute();

        getEntityManager().clear();
    }
}
