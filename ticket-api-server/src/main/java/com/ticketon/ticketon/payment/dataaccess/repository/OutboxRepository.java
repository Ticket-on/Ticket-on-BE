package com.ticketon.ticketon.payment.dataaccess.repository;

import com.ticketon.ticketon.payment.dataaccess.entity.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<OutboxMessage, Long> {
}
