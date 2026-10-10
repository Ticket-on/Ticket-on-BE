package com.ticketon.ticketon.payment.dataaccess.entity;

import com.ticketon.ticketon.payment.vo.PaymentConfirmation;
import com.ticketon.ticketon.payment.vo.PaymentMessage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id", nullable = false)
    private Long id;

    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "ticket_type_id")
    private Long ticketTypeId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "orderId", nullable = false, unique = true)
    private String orderId;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "payment_key", nullable = false)
    private String paymentKey;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Builder
    public Payment(Long ticketId, Long ticketTypeId, Long memberId, String orderId, PaymentStatus paymentStatus,
                   Integer amount, String paymentKey, LocalDateTime requestedAt, LocalDateTime approvedAt) {
        this.ticketId = ticketId;
        this.ticketTypeId = ticketTypeId;
        this.memberId = memberId;
        this.orderId = orderId;
        this.paymentStatus = paymentStatus;
        this.amount = amount;
        this.paymentKey = paymentKey;
        this.requestedAt = requestedAt;
        this.approvedAt = approvedAt;
    }

    public static Payment success(PaymentMessage message, Long ticketId, Long ticketTypeId) {
        return Payment.builder()
                .ticketId(ticketId)
                .ticketTypeId(ticketTypeId)
                .orderId(message.orderId())
                .memberId(message.memberId())
                .amount(message.amount())
                .paymentKey(message.paymentKey())
                .paymentStatus(PaymentStatus.SUCCESS)
                .requestedAt(message.requestedAt())
                .approvedAt(message.approvedAt())
                .build();
    }

    public static Payment unknown(PaymentConfirmation confirmation, Long ticketTypeId, LocalDateTime requestedAt) {
        return Payment.builder()
                .ticketTypeId(ticketTypeId)
                .orderId(confirmation.orderId())
                .memberId(confirmation.memberId())
                .amount(confirmation.amount())
                .paymentKey(confirmation.paymentKey())
                .paymentStatus(PaymentStatus.UNKNOWN)
                .requestedAt(requestedAt)
                .build();
    }

    public void approve(PaymentMessage message, Long ticketId) {
        this.ticketId = ticketId;
        this.paymentKey = message.paymentKey();
        this.amount = message.amount();
        this.approvedAt = message.approvedAt();
        this.paymentStatus = PaymentStatus.SUCCESS;
    }

    public void cancel(LocalDateTime canceledAt) {
        this.paymentStatus = PaymentStatus.CANCELED;
        this.canceledAt = canceledAt;
    }
}
