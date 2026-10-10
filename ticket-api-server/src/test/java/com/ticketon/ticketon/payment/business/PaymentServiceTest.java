package com.ticketon.ticketon.payment.business;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.payment.implement.PaymentManager;
import com.ticketon.ticketon.payment.implement.PaymentReader;
import com.ticketon.ticketon.payment.implement.gateway.PaymentGateway;
import com.ticketon.ticketon.payment.implement.outbox.OutboxWriter;
import com.ticketon.ticketon.payment.vo.PaymentApproval;
import com.ticketon.ticketon.payment.vo.PaymentConfirmation;
import com.ticketon.ticketon.payment.vo.PaymentInquiry;
import com.ticketon.ticketon.payment.vo.PaymentMessage;
import com.ticketon.ticketon.payment.vo.PaymentUnknownEvent;
import com.ticketon.ticketon.payment.vo.PgError;
import com.ticketon.ticketon.ticket.implement.PendingTicketManager;
import com.ticketon.ticketon.ticket.vo.PendingTicket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

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
    private final PaymentUnknownEvent unknownEvent = new PaymentUnknownEvent(ORDER_ID, "pk", 1L, 10000);

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

    @Test
    @DisplayName("UNKNOWN 조회 결과가 승인이면 결제 완료 처리로 넘긴다")
    void resolveUnknownPaid() {
        LocalDateTime now = LocalDateTime.now();
        when(paymentGateway.inquire(ORDER_ID)).thenReturn(new PaymentInquiry(ORDER_ID, "pk", 10000, "DONE", now, now));

        paymentService.resolveUnknown(unknownEvent);

        verify(paymentManager).complete(List.of(new PaymentMessage("pk", 1L, ORDER_ID, 10000, now, now)));
        verify(paymentManager, never()).failUnknown(any());
    }

    @Test
    @DisplayName("UNKNOWN 조회 결과가 만료면 FAILED 로 바꾸고 재결제할 수 있게 마커를 지운다")
    void resolveUnknownFailed() {
        when(paymentGateway.inquire(ORDER_ID)).thenReturn(inquiry("EXPIRED", 10000));

        paymentService.resolveUnknown(unknownEvent);

        verify(paymentManager).failUnknown(ORDER_ID);
        verify(pendingTicketManager).clearUnknown(ORDER_ID);
        verify(paymentManager, never()).complete(any());
    }

    @Test
    @DisplayName("아직 결과가 없으면 PAYMENT_RESULT_UNKNOWN 을 던져 다음 retry 토픽으로 넘긴다")
    void resolveUnknownPending() {
        when(paymentGateway.inquire(ORDER_ID)).thenReturn(PaymentInquiry.notFound(ORDER_ID));

        assertThatThrownBy(() -> paymentService.resolveUnknown(unknownEvent))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorType()).isEqualTo(ErrorType.PAYMENT_RESULT_UNKNOWN));

        verify(paymentManager, never()).complete(any());
        verify(paymentManager, never()).failUnknown(any());
    }

    @Test
    @DisplayName("금액이 다르면 자동으로 확정하지 않는다")
    void resolveUnknownAmountMismatch() {
        when(paymentGateway.inquire(ORDER_ID)).thenReturn(inquiry("DONE", 9999));

        paymentService.resolveUnknown(unknownEvent);

        verify(paymentManager, never()).complete(any());
        verify(paymentManager, never()).failUnknown(any());
    }

    @Test
    @DisplayName("조회 자체가 실패하면 예외를 그대로 던져 retry 한다")
    void resolveUnknownInquiryFailure() {
        when(paymentGateway.inquire(ORDER_ID)).thenThrow(new AppException(ErrorType.PG_UNAVAILABLE));

        assertThatThrownBy(() -> paymentService.resolveUnknown(unknownEvent))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorType()).isEqualTo(ErrorType.PG_UNAVAILABLE));
    }

    private PaymentInquiry inquiry(String status, int totalAmount) {
        LocalDateTime now = LocalDateTime.now();
        return new PaymentInquiry(ORDER_ID, "pk", totalAmount, status, now, now);
    }
}
