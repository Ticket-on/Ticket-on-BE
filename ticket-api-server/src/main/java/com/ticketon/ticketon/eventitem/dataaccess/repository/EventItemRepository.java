package com.ticketon.ticketon.eventitem.dataaccess.repository;

import com.ticketon.ticketon.eventitem.dataaccess.entity.EventItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventItemRepository extends JpaRepository<EventItem, Long> {
    List<EventItem> findAllByOrderByStartDateAsc();
    Optional<EventItem> findByTicketTypes_id(Long ticketTypeId);
}
