package com.ticketon.ticketon.ticket.dataaccess.repository;

import com.ticketon.ticketon.ticket.dataaccess.entity.Ticket;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TicketJdbcRepository {

    private static final String INSERT_SQL =
            "INSERT INTO tickets (ticket_type_id, member_id, status, price, order_id, expired_at) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    public void batchInsert(List<Ticket> tickets) {
        jdbcTemplate.batchUpdate(INSERT_SQL, tickets, tickets.size(), (ps, ticket) -> {
            ps.setLong(1, ticket.getTicketTypeId());
            ps.setLong(2, ticket.getMemberId());
            ps.setString(3, ticket.getTicketStatus().name());
            ps.setInt(4, ticket.getPrice());
            ps.setString(5, ticket.getOrderId());
            ps.setTimestamp(6, Timestamp.valueOf(ticket.getExpiredAt()));
        });
    }
}
