package com.ticketon.ticketon.eventitem.dataaccess.entity;

import com.ticketon.ticketon.ticket.dataaccess.entity.TicketType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Table(name = "event_items")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_item_id", nullable = false)
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @OneToMany(mappedBy = "eventItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TicketType> ticketTypes = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "event_status", nullable = false)
    private EventItemStatus eventItemStatus;

    @Builder
    public EventItem(String title, LocalDate startDate, LocalDate endDate, EventItemStatus eventItemStatus) {
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.eventItemStatus = eventItemStatus;
    }
}
