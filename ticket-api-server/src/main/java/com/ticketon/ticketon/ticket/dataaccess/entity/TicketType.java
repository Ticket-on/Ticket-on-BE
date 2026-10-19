package com.ticketon.ticketon.ticket.dataaccess.entity;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.eventitem.dataaccess.entity.EventItem;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "ticket_types")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_type_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventItem eventItem;

    @Column(name = "ticket_type_name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "max_quantity", nullable = false)
    private Long maxQuantity;

    @Column(name = "issued_quantity", nullable = false)
    private Long issuedQuantity;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TicketTypeStatus status;

    @Builder
    public TicketType(EventItem eventItem, String name, String description, Long maxQuantity,
                      Long issuedQuantity, Integer price, TicketTypeStatus status) {
        this.eventItem = eventItem;
        this.name = name;
        this.description = description;
        this.maxQuantity = maxQuantity;
        this.issuedQuantity = issuedQuantity;
        this.price = price;
        this.status = status;
    }

    public void increaseIssuedQuantity() {
        if (this.issuedQuantity >= this.maxQuantity) {
            throw new AppException(ErrorType.TICKET_SOLD_OUT);
        }
        this.issuedQuantity++;
    }

    public void decreaseIssuedQuantity() {
        if (this.issuedQuantity < 1) {
            throw new AppException(ErrorType.NO_ISSUED_TICKET_TO_CANCEL);
        }
        this.issuedQuantity--;
    }
}
