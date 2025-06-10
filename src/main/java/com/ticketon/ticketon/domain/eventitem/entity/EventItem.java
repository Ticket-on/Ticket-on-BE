package com.ticketon.ticketon.domain.eventitem.entity;

import com.ticketon.ticketon.domain.ticket.entity.TicketType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Table(name = "event_items")
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_item_id", nullable = false)
    private Long id;

    // 공연 제목 Ex) 2025 싸이의 흠뻑쇼
    @Column(name = "title", nullable = false)
    private String title;

    // 공연 시작 날짜
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    // 마지막 공연 날짜
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_status", nullable = false)
    private EventItemStatus eventItemStatus;

    @OneToMany(mappedBy = "eventItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TicketType> ticketTypes;
}
