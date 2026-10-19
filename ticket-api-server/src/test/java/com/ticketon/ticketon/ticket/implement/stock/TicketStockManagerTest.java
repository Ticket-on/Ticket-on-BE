package com.ticketon.ticketon.ticket.implement.stock;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.ticket.dataaccess.repository.TicketStockRedisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketStockManagerTest {

    private static final Long TICKET_TYPE_ID = 1L;

    private TicketStockRedisRepository repository;
    private TicketStockManager manager;

    @BeforeEach
    void setUp() {
        repository = mock(TicketStockRedisRepository.class);
        manager = new TicketStockManager(repository);
    }

    @Test
    @DisplayName("Lua 결과가 1 이면 차감에 성공한다")
    void decreased() {
        when(repository.decrease(eq(TICKET_TYPE_ID), anyInt())).thenReturn(1L);

        assertThatCode(() -> manager.decrease(TICKET_TYPE_ID)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("재고 키가 없으면(-1) 시스템 오류로 본다")
    void notInitialized() {
        when(repository.decrease(eq(TICKET_TYPE_ID), anyInt())).thenReturn(-1L);

        assertThatThrownBy(() -> manager.decrease(TICKET_TYPE_ID))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorType()).isEqualTo(ErrorType.TICKET_STOCK_NOT_INITIALIZED));
    }

    @Test
    @DisplayName("한 번 매진되면 이후 요청은 Redis 를 호출하지 않고 바로 거절한다")
    void soldOutIsCached() {
        when(repository.decrease(eq(TICKET_TYPE_ID), anyInt())).thenReturn(0L);

        assertSoldOut();
        assertSoldOut();

        verify(repository, times(1)).decrease(eq(TICKET_TYPE_ID), anyInt());
    }

    @Test
    @DisplayName("재고가 복구되면 매진 표시를 해제하고 다시 Redis 로 차감을 시도한다")
    void restoreClearsSoldOut() {
        when(repository.decrease(eq(TICKET_TYPE_ID), anyInt())).thenReturn(0L, 1L);
        assertSoldOut();

        manager.restore(TICKET_TYPE_ID);

        assertThatCode(() -> manager.decrease(TICKET_TYPE_ID)).doesNotThrowAnyException();
        verify(repository, times(2)).decrease(eq(TICKET_TYPE_ID), anyInt());
    }

    private void assertSoldOut() {
        assertThatThrownBy(() -> manager.decrease(TICKET_TYPE_ID))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorType()).isEqualTo(ErrorType.TICKET_SOLD_OUT));
    }
}
