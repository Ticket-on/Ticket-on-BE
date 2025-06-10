package com.ticketon.ticketon.domain.eventitem.entity.dto;

import com.ticketon.ticketon.domain.eventitem.entity.EventItem;
import com.ticketon.ticketon.domain.eventitem.entity.EventItemStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class EventItemCreateRequest {
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;

    public EventItem toEntity() {
        return EventItem.builder()
                .title(this.getTitle())
                .startDate(this.getStartDate())
                .endDate(this.getEndDate())
                .eventItemStatus(EventItemStatus.HIDDEN)
                .build();
    }
}
