package com.ticketon.ticketon.payment.dataaccess.repository;

import com.ticketon.ticketon.payment.dataaccess.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTicketId(Long ticketId);

    Optional<Payment> findByPaymentKey(String paymentKey);
}
