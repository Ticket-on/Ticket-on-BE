package com.ticketon.ticketon.ticket.dataaccess.repository;

import com.ticketon.ticketon.ticket.dataaccess.entity.Ticket;
import com.ticketon.ticketon.ticket.dataaccess.repository.custom.TicketCustomRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long>, TicketCustomRepository {
    List<Ticket> findByMember_Id(Long memberId);

    List<Ticket> findAllByOrderIdIn(Collection<String> orderIds);
}
