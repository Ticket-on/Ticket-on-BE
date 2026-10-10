package com.ticketon.ticketon.payment.implement;

import com.ticketon.ticketon.payment.dataaccess.entity.PaymentStatus;
import com.ticketon.ticketon.payment.dataaccess.repository.PaymentRepository;
import com.ticketon.ticketon.payment.implement.outbox.OutboxWriter;
import com.ticketon.ticketon.payment.vo.PaymentMessage;
import com.ticketon.ticketon.ticket.dataaccess.entity.Ticket;
import com.ticketon.ticketon.ticket.implement.TicketReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentManagerTest {

    private static final String ORDER_ID = "order-1";

    private final PaymentReader paymentReader = mock(PaymentReader.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final TicketReader ticketReader = mock(TicketReader.class);
    private final PaymentManager paymentManager =
            new PaymentManager(paymentReader, paymentRepository, ticketReader, mock(OutboxWriter.class));

    private final LocalDateTime now = LocalDateTime.now();
    private final PaymentMessage message = new PaymentMessage("pk", 1L, ORDER_ID, 10000, now, now);
    private final Ticket ticket = mock(Ticket.class);

    @BeforeEach
    void setUp() {
        when(ticket.getId()).thenReturn(10L);
        when(ticket.isPending()).thenReturn(true);
        when(ticketReader.findAllByOrderIds(anyList())).thenReturn(Map.of(ORDER_ID, ticket));
    }

    @Test
    @DisplayName("결제 행이 없으면 SUCCESS 로 새로 저장하고 티켓을 확정한다")
    void insertsWhenNoPayment() {
        when(paymentReader.findStatusesByOrderIds(anyList())).thenReturn(Map.of());

        assertThat(paymentManager.complete(List.of(message))).isEqualTo(1);

        verify(paymentRepository).saveAll(argThat(payments -> payments.iterator().hasNext()));
        verify(ticket).markAsConfirmed();
    }

    @Test
    @DisplayName("UNKNOWN 행이 있으면 조건부 UPDATE 로 SUCCESS 로 바꾼다")
    void approvesUnknown() {
        when(paymentReader.findStatusesByOrderIds(anyList())).thenReturn(Map.of(ORDER_ID, PaymentStatus.UNKNOWN));
        when(paymentRepository.approveUnknown(ORDER_ID, 10L, "pk", 10000, now)).thenReturn(1);

        assertThat(paymentManager.complete(List.of(message))).isEqualTo(1);

        verify(paymentRepository).saveAll(argThat(payments -> !payments.iterator().hasNext()));
        verify(ticket).markAsConfirmed();
    }

    @Test
    @DisplayName("조건부 UPDATE 에서 지면(다른 쪽이 먼저 확정) 티켓을 건드리지 않는다")
    void losesRace() {
        when(paymentReader.findStatusesByOrderIds(anyList())).thenReturn(Map.of(ORDER_ID, PaymentStatus.UNKNOWN));
        when(paymentRepository.approveUnknown(any(), any(), any(), any(), any())).thenReturn(0);

        assertThat(paymentManager.complete(List.of(message))).isZero();

        verify(ticket, never()).markAsConfirmed();
    }

    @Test
    @DisplayName("이미 FAILED 로 확정된 결제에 승인 메시지가 오면 자동으로 바꾸지 않는다")
    void doesNotOverrideFailed() {
        when(paymentReader.findStatusesByOrderIds(anyList())).thenReturn(Map.of(ORDER_ID, PaymentStatus.FAILED));

        assertThat(paymentManager.complete(List.of(message))).isZero();

        verify(paymentRepository, never()).approveUnknown(any(), any(), any(), any(), any());
        verify(ticket, never()).markAsConfirmed();
    }
}
