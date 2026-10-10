package com.ticketon.ticketon.payment.dataaccess.repository;

import com.ticketon.ticketon.payment.dataaccess.entity.Payment;
import com.ticketon.ticketon.payment.dataaccess.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTicketId(Long ticketId);

    Optional<Payment> findByPaymentKey(String paymentKey);

    List<Payment> findAllByOrderIdIn(Collection<String> orderIds);

    List<Payment> findAllByOrderIdInAndPaymentStatus(Collection<String> orderIds, PaymentStatus paymentStatus);

    @Modifying
    @Query("""
            update Payment p
               set p.paymentStatus = com.ticketon.ticketon.payment.dataaccess.entity.PaymentStatus.SUCCESS,
                   p.ticketId = :ticketId,
                   p.paymentKey = :paymentKey,
                   p.amount = :amount,
                   p.approvedAt = :approvedAt
             where p.orderId = :orderId
               and p.paymentStatus = com.ticketon.ticketon.payment.dataaccess.entity.PaymentStatus.UNKNOWN
            """)
    int approveUnknown(@Param("orderId") String orderId,
                       @Param("ticketId") Long ticketId,
                       @Param("paymentKey") String paymentKey,
                       @Param("amount") Integer amount,
                       @Param("approvedAt") LocalDateTime approvedAt);

    @Modifying
    @Query("""
            update Payment p
               set p.paymentStatus = com.ticketon.ticketon.payment.dataaccess.entity.PaymentStatus.FAILED
             where p.orderId = :orderId
               and p.paymentStatus = com.ticketon.ticketon.payment.dataaccess.entity.PaymentStatus.UNKNOWN
            """)
    int failUnknown(@Param("orderId") String orderId);
}
