package com.ticketon.ticketon.ticket.dataaccess.entity;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketTest {

    @Test
    @DisplayName("이미 취소된 티켓을 다시 취소하면 TICKET_ALREADY_CANCELLED")
    void cancelTwice() {
        Ticket ticket = Ticket.builder().ticketStatus(TicketStatus.PENDING).build();
        ticket.cancel();

        assertThat(ticket.getTicketStatus()).isEqualTo(TicketStatus.CANCELLED);
        assertThatThrownBy(ticket::cancel)
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorType()).isEqualTo(ErrorType.TICKET_ALREADY_CANCELLED));
    }

    @Test
    @DisplayName("최대 수량까지 발급하면 이후 발급은 TICKET_SOLD_OUT")
    void increaseOverMax() {
        TicketType ticketType = TicketType.builder().maxQuantity(1L).issuedQuantity(0L).build();
        ticketType.increaseIssuedQuantity();

        assertThatThrownBy(ticketType::increaseIssuedQuantity)
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorType()).isEqualTo(ErrorType.TICKET_SOLD_OUT));
    }

    @Test
    @DisplayName("발급된 티켓이 없으면 수량을 줄일 수 없다")
    void decreaseBelowZero() {
        TicketType ticketType = TicketType.builder().maxQuantity(1L).issuedQuantity(0L).build();

        assertThatThrownBy(ticketType::decreaseIssuedQuantity)
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorType()).isEqualTo(ErrorType.NO_ISSUED_TICKET_TO_CANCEL));
    }
}
