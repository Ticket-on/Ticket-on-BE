package com.ticketon.ticketon.payment.business;

import com.ticketon.ticketon.payment.implement.PaymentManager;
import com.ticketon.ticketon.payment.implement.PaymentReader;
import com.ticketon.ticketon.payment.implement.gateway.PaymentGateway;
import com.ticketon.ticketon.payment.implement.outbox.OutboxWriter;
import com.ticketon.ticketon.payment.vo.PaymentApproval;
import com.ticketon.ticketon.payment.vo.PaymentCancellation;
import com.ticketon.ticketon.payment.vo.PaymentConfirmation;
import com.ticketon.ticketon.payment.vo.PaymentInfo;
import com.ticketon.ticketon.payment.vo.PaymentMessage;
import com.ticketon.ticketon.ticket.implement.PendingTicketManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        pendingTicketManager.getPayable(confirmation.orderId());

        LocalDateTime now = LocalDateTime.now();
        PaymentApproval approval = new PaymentApproval("test-key", confirmation.orderId(), 10000, now, now);
//        PaymentApproval approval = paymentGateway.requestPaymentConfirm(confirmation);
        PaymentMessage message = PaymentMessage.of(confirmation, approval);

        pendingTicketManager.markPaid(confirmation.orderId());
        outboxWriter.savePaymentConfirmed(message);
        log.info("[결제 승인 : 완료]: orderId={} | memberId={}", confirmation.orderId(), confirmation.memberId());
        return message;
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
