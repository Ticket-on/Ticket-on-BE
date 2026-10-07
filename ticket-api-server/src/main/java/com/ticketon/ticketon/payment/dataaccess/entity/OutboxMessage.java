package com.ticketon.ticketon.payment.dataaccess.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "outbox_message")
public class OutboxMessage {

    private static final String PAYMENT_CONFIRM_TOPIC = "payment-confirm";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbox_message_id")
    private Long id;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Builder
    public OutboxMessage(String topic, String payload) {
        this.topic = topic;
        this.payload = payload;
    }

    public static OutboxMessage paymentConfirmed(String payload) {
        return OutboxMessage.builder()
                .topic(PAYMENT_CONFIRM_TOPIC)
                .payload(payload)
                .build();
    }
}
