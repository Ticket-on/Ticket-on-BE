package com.ticketon.ticketon.ticket.dataaccess.entity;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.member.dataaccess.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Table(name = "tickets")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket {

    private static final long PENDING_EXPIRE_MINUTES = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_type_id", nullable = false)
    private TicketType ticketType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TicketStatus ticketStatus;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Builder
    public Ticket(TicketType ticketType, Member member, TicketStatus ticketStatus,
                  Integer price, String orderId, LocalDateTime expiredAt) {
        this.ticketType = ticketType;
        this.member = member;
        this.ticketStatus = ticketStatus;
        this.price = price;
        this.orderId = orderId;
        this.expiredAt = expiredAt;
    }

    public static Ticket createPending(TicketType ticketType, Member member, String orderId) {
        return Ticket.builder()
                .ticketType(ticketType)
                .member(member)
                .price(ticketType.getPrice())
                .orderId(orderId)
                .ticketStatus(TicketStatus.PENDING)
                .expiredAt(LocalDateTime.now().plusMinutes(PENDING_EXPIRE_MINUTES))
                .build();
    }

    public Long getTicketTypeId() {
        return ticketType.getId();
    }

    public Long getMemberId() {
        return member.getId();
    }

    public boolean isPending() {
        return ticketStatus == TicketStatus.PENDING;
    }

    public boolean isOwnedBy(Long memberId) {
        return Objects.equals(getMemberId(), memberId);
    }

    public void markAsConfirmed() {
        this.ticketStatus = TicketStatus.CONFIRMED;
    }

    public void cancel() {
        if (this.ticketStatus == TicketStatus.CANCELLED) {
            throw new AppException(ErrorType.TICKET_ALREADY_CANCELLED);
        }
        this.ticketStatus = TicketStatus.CANCELLED;
    }
}
