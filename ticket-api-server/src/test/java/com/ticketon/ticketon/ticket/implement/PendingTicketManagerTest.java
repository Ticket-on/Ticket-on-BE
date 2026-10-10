package com.ticketon.ticketon.ticket.implement;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.ticket.dataaccess.repository.PendingTicketRedisRepository;
import com.ticketon.ticketon.ticket.vo.PendingTicket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PendingTicketManagerTest {

    private static final String ORDER_ID = "order-1";

    private final PendingTicketRedisRepository repository = mock(PendingTicketRedisRepository.class);
    private final PendingTicketManager manager = new PendingTicketManager(repository);

    @Test
    @DisplayName("홀드가 있고 결과 불명이 아니면 결제할 수 있다")
    void payable() {
        PendingTicket pendingTicket = PendingTicket.of(ORDER_ID, 7L, 1L);
        when(repository.find(ORDER_ID)).thenReturn(Optional.of(pendingTicket));

        assertThat(manager.getPayable(ORDER_ID)).isEqualTo(pendingTicket);
    }

    @Test
    @DisplayName("홀드가 만료됐으면 PAYMENT_RESERVATION_EXPIRED")
    void expired() {
        when(repository.find(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> manager.getPayable(ORDER_ID))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorType()).isEqualTo(ErrorType.PAYMENT_RESERVATION_EXPIRED));
    }

    @Test
    @DisplayName("결과 불명인 주문은 다시 승인하지 않고 PAYMENT_RESULT_UNKNOWN")
    void unknown() {
        when(repository.find(ORDER_ID)).thenReturn(Optional.of(PendingTicket.of(ORDER_ID, 7L, 1L)));
        when(repository.isUnknown(ORDER_ID)).thenReturn(true);

        assertThatThrownBy(() -> manager.getPayable(ORDER_ID))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorType()).isEqualTo(ErrorType.PAYMENT_RESULT_UNKNOWN));
    }
}
