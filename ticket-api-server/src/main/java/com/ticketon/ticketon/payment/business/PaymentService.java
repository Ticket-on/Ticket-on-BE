package com.ticketon.ticketon.payment.business;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.payment.dataaccess.entity.PaymentStatus;
import com.ticketon.ticketon.payment.implement.PaymentManager;
import com.ticketon.ticketon.payment.implement.PaymentReader;
import com.ticketon.ticketon.payment.implement.gateway.PaymentGateway;
import com.ticketon.ticketon.payment.implement.outbox.OutboxWriter;
import com.ticketon.ticketon.payment.vo.PaymentApproval;
import com.ticketon.ticketon.payment.vo.PaymentCancellation;
import com.ticketon.ticketon.payment.vo.PaymentConfirmation;
import com.ticketon.ticketon.payment.vo.PaymentInfo;
import com.ticketon.ticketon.payment.vo.PaymentInquiry;
import com.ticketon.ticketon.payment.vo.PaymentMessage;
import com.ticketon.ticketon.payment.vo.PaymentUnknownEvent;
import com.ticketon.ticketon.payment.vo.UnknownResolution;
import com.ticketon.ticketon.ticket.implement.PendingTicketManager;
import com.ticketon.ticketon.ticket.vo.PendingTicket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentGateway paymentGateway;
    private final PaymentReader paymentReader;
    private final PaymentManager paymentManager;
    private final OutboxWriter outboxWriter;
    private final PendingTicketManager pendingTicketManager;

    public PaymentMessage confirm(PaymentConfirmation confirmation) {
        PendingTicket pendingTicket = pendingTicketManager.getPayable(confirmation.orderId());

        PaymentApproval approval = requestConfirm(confirmation, pendingTicket);
        PaymentMessage message = PaymentMessage.of(confirmation, approval);

        pendingTicketManager.markPaid(confirmation.orderId());
        outboxWriter.savePaymentConfirmed(message);
        log.info("[결제 승인 : 완료]: orderId={} | memberId={}", confirmation.orderId(), confirmation.memberId());
        return message;
    }

    private PaymentApproval requestConfirm(PaymentConfirmation confirmation, PendingTicket pendingTicket) {
        LocalDateTime requestedAt = LocalDateTime.now();
        try {
            return paymentGateway.requestPaymentConfirm(confirmation);
        } catch (AppException e) {
            if (PaymentStatus.ofConfirmFailure(e.getErrorType()) == PaymentStatus.FAILED) {
                log.warn("[결제 승인 : 실패 확정]: orderId={} | errorType={}", confirmation.orderId(), e.getErrorType());
                throw e;
            }
            saveUnknown(confirmation, pendingTicket, requestedAt, e);
            throw new AppException(ErrorType.PAYMENT_RESULT_UNKNOWN, e.getData(), e);
        }
    }

    private void saveUnknown(PaymentConfirmation confirmation, PendingTicket pendingTicket, LocalDateTime requestedAt, AppException cause) {
        try {
            paymentManager.saveUnknown(confirmation, pendingTicket.ticketTypeId(), requestedAt);
            log.warn("[결제 승인 : 결과 불명 저장]: orderId={} | errorType={}", confirmation.orderId(), cause.getErrorType());
        } catch (DataIntegrityViolationException duplicate) {
            log.warn("[결제 승인 : 이미 결과 확인 중]: orderId={} | errorType={}", confirmation.orderId(), cause.getErrorType());
        }
    }

    public void resolveUnknown(PaymentUnknownEvent event) {
        PaymentInquiry inquiry = paymentGateway.inquire(event.orderId());
        UnknownResolution resolution = inquiry.resolve(event.amount());

        switch (resolution) {
            case PAID -> {
                int completedCount = paymentManager.complete(List.of(inquiry.toPaymentMessage(event.memberId())));
                log.info("[결제 UNKNOWN 확정 : 승인]: orderId={} | completed={}", event.orderId(), completedCount);
            }
            case FAILED -> failUnknown(event, inquiry);
            case CANCELED -> {
                log.error("[결제 UNKNOWN 확정 : 외부 취소]: orderId={} | status={} | 수동 확인 필요",
                        event.orderId(), inquiry.status());
                failUnknown(event, inquiry);
            }
            case AMOUNT_MISMATCH -> log.error("[결제 UNKNOWN 확정 : 금액 불일치]: orderId={} | expected={} | actual={} | 수동 확인 필요",
                    event.orderId(), event.amount(), inquiry.totalAmount());
            case PENDING -> {
                log.info("[결제 UNKNOWN 확정 : 결과 대기]: orderId={} | status={}", event.orderId(), inquiry.status());
                throw new AppException(ErrorType.PAYMENT_RESULT_UNKNOWN);
            }
        }
    }

    private void failUnknown(PaymentUnknownEvent event, PaymentInquiry inquiry) {
        boolean failed = paymentManager.failUnknown(event.orderId());
        pendingTicketManager.delete(event.orderId());
        log.info("[결제 UNKNOWN 확정 : 미승인]: orderId={} | status={} | updated={}", event.orderId(), inquiry.status(), failed);
    }

    public void cancel(PaymentCancellation cancellation) {
        paymentReader.findInfoByPaymentKey(cancellation.paymentKey());
        LocalDateTime canceledAt = paymentGateway.requestPaymentCancel(cancellation);
        paymentManager.cancel(cancellation.paymentKey(), canceledAt);
        log.info("[결제 취소 : 완료]: paymentKey={} | amount={}", cancellation.paymentKey(), cancellation.cancelAmount());
    }

    public PaymentInfo getByTicketId(Long ticketId) {
        return paymentReader.findInfoByTicketId(ticketId);
    }

    public void complete(List<PaymentMessage> messages) {
        int savedCount = paymentManager.complete(messages);
        log.info("[결제 완료 처리 : 티켓 확정]: received={} | saved={}", messages.size(), savedCount);
    }
}
