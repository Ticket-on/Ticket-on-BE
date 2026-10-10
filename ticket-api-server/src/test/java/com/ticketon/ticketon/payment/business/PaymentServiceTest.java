package com.ticketon.ticketon.payment.business;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.payment.implement.PaymentManager;
import com.ticketon.ticketon.payment.implement.PaymentReader;
import com.ticketon.ticketon.payment.implement.gateway.PaymentGateway;
import com.ticketon.ticketon.payment.implement.outbox.OutboxWriter;
import com.ticketon.ticketon.payment.vo.PaymentApproval;
import com.ticketon.ticketon.payment.vo.PaymentConfirmation;
import com.ticketon.ticketon.payment.vo.PgError;
import com.ticketon.ticketon.ticket.implement.PendingTicketManager;
import com.ticketon.ticketon.ticket.vo.PendingTicket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private static final String ORDER_ID = "order-1";
    private static final Long TICKET_TYPE_ID = 7L;

    private final PaymentConfirmation confirmation = new PaymentConfirmation("pk", 1L, ORDER_ID, 10000);
    private final PgError pgError = new PgError("SOME_CODE", "msg");

    private PaymentGateway paymentGateway;
    private PaymentManager paymentManager;
    private OutboxWriter outboxWriter;
    private PendingTicketManager pendingTicketManager;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentGateway = mock(PaymentGateway.class);
        paymentManager = mock(PaymentManager.class);
        outboxWriter = mock(OutboxWriter.class);
        pendingTicketManager = mock(PendingTicketManager.class);
        paymentService = new PaymentService(paymentGateway, mock(PaymentReader.class), paymentManager,
                outboxWriter, pendingTicketManager);

        when(pendingTicketManager.getPayable(ORDER_ID)).thenReturn(PendingTicket.of(ORDER_ID, TICKET_TYPE_ID, 1L));
    }

    @Test
    @DisplayName("승인되면 결제 완료 표시와 아웃박스 저장을 하고 UNKNOWN 은 남기지 않는다")
    void approved() {
        LocalDateTime now = LocalDateTime.now();
        when(paymentGateway.requestPaymentConfirm(confirmation))
                .thenReturn(new PaymentApproval("pk", ORDER_ID, 10000, now, now));

        paymentService.confirm(confirmation);

        verify(pendingTicketManager).markPaid(ORDER_ID);
        verify(outboxWriter).savePaymentConfirmed(any());
        verify(paymentManager, never()).saveUnknown(any(), any(), any());
    }

    @Test
    @DisplayName("승인 거절이 확정되면 UNKNOWN 을 남기지 않고 PAYMENT_REJECTED 를 그대로 던진다")
    void rejected() {
        when(paymentGateway.requestPaymentConfirm(confirmation))
                .thenThrow(new AppException(ErrorType.PAYMENT_REJECTED, pgError));

        assertThatThrownBy(() -> paymentService.confirm(confirmation))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorType()).isEqualTo(ErrorType.PAYMENT_REJECTED));

        verify(paymentManager, never()).saveUnknown(any(), any(), any());
        verify(pendingTicketManager, never()).markUnknown(any());
        verify(pendingTicketManager, never()).markPaid(any());
    }

    @Test
    @DisplayName("PG 경로 이상이면 UNKNOWN 을 저장하고 사용자에게는 PAYMENT_RESULT_UNKNOWN 으로 응답한다")
    void pgUnavailableBecomesUnknown() {
        when(paymentGateway.requestPaymentConfirm(confirmation))
                .thenThrow(new AppException(ErrorType.PG_UNAVAILABLE, pgError));

        assertThatThrownBy(() -> paymentService.confirm(confirmation))
                .isInstanceOfSatisfying(AppException.class, e -> {
                    assertThat(e.getErrorType()).isEqualTo(ErrorType.PAYMENT_RESULT_UNKNOWN);
                    assertThat(e.getData()).isEqualTo(pgError);
                });

        verify(paymentManager).saveUnknown(eq(confirmation), eq(TICKET_TYPE_ID), any());
        verify(pendingTicketManager).markUnknown(ORDER_ID);
        verify(pendingTicketManager, never()).markPaid(any());
        verify(outboxWriter, never()).savePaymentConfirmed(any());
    }

    @Test
    @DisplayName("PG 결과 불명이면 UNKNOWN 을 저장한다")
    void resultUnknown() {
        when(paymentGateway.requestPaymentConfirm(confirmation))
                .thenThrow(new AppException(ErrorType.PAYMENT_RESULT_UNKNOWN, pgError));

        assertThatThrownBy(() -> paymentService.confirm(confirmation))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorType()).isEqualTo(ErrorType.PAYMENT_RESULT_UNKNOWN));

        verify(paymentManager).saveUnknown(eq(confirmation), eq(TICKET_TYPE_ID), any());
    }
}
