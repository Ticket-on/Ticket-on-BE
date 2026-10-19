package com.ticketon.ticketon.ticket.dataaccess.repository;

import com.ticketon.ticketon.ticket.dataaccess.entity.TicketType;
import com.ticketon.ticketon.ticket.dataaccess.repository.custom.TicketTypeCustomRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketTypeRepository extends JpaRepository<TicketType, Long>, TicketTypeCustomRepository {

}
