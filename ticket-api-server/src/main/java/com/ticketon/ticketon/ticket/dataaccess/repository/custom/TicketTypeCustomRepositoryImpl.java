package com.ticketon.ticketon.ticket.dataaccess.repository.custom;

import com.ticketon.ticketon.support.repository.QuerydslRepositorySupport;
import com.ticketon.ticketon.ticket.dataaccess.entity.TicketType;
import jakarta.persistence.LockModeType;

import java.util.Optional;

import static com.ticketon.ticketon.ticket.dataaccess.entity.QTicketType.ticketType;

public class TicketTypeCustomRepositoryImpl extends QuerydslRepositorySupport implements TicketTypeCustomRepository {

    private static final int LOCK_TIMEOUT_MILLIS = 3000;

    protected TicketTypeCustomRepositoryImpl() {
        super(TicketType.class);
    }

    @Override
    public Optional<TicketType> findByIdForUpdate(Long ticketTypeId) {
        return Optional.ofNullable(
                selectFrom(ticketType)
                        .where(ticketType.id.eq(ticketTypeId))
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .setHint("jakarta.persistence.lock.timeout", LOCK_TIMEOUT_MILLIS)
                        .fetchOne()
        );
    }

    @Override
    public long increaseTicketAtomically(Long ticketTypeId) {
        long updatedRows = update(ticketType)
                .set(ticketType.issuedQuantity, ticketType.issuedQuantity.add(1))
                .where(ticketType.id.eq(ticketTypeId),
                        ticketType.maxQuantity.gt(ticketType.issuedQuantity))
                .execute();

        getEntityManager().clear();
        return updatedRows;
    }
}
