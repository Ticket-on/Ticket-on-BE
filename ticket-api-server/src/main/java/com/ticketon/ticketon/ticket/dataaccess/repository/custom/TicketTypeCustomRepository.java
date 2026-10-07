package com.ticketon.ticketon.ticket.dataaccess.repository.custom;

import com.ticketon.ticketon.ticket.dataaccess.entity.TicketType;

import java.util.Optional;

public interface TicketTypeCustomRepository {
    Optional<TicketType> findByIdForUpdate(Long ticketTypeId);
    long increaseTicketAtomically(Long ticketTypeId);
}
