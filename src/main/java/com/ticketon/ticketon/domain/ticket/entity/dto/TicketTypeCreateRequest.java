package com.ticketon.ticketon.domain.ticket.entity.dto;

import com.ticketon.ticketon.domain.eventitem.entity.EventItem;
import com.ticketon.ticketon.domain.ticket.entity.TicketType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketTypeCreateRequest {
    private String name;
    private String description;
    private Long price;
    private Long maxQuantity;

    public TicketType toEntityWithEventItem(EventItem eventItem) {
        return TicketType.builder()
                .eventItem(eventItem)
                .name(this.name)
                .description(this.description)
                .price(this.price)
                .maxQuantity(this.maxQuantity)
                .issuedQuantity(0L)
                .status(TicketTypeStatus.READY)
                .build();
    }
}
