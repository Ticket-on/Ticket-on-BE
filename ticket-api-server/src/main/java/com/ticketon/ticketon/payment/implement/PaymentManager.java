package com.ticketon.ticketon.payment.implement;

import com.ticketon.ticketon.payment.dataaccess.entity.Payment;
import com.ticketon.ticketon.payment.dataaccess.entity.PaymentStatus;
import com.ticketon.ticketon.payment.dataaccess.repository.PaymentRepository;
import com.ticketon.ticketon.payment.implement.outbox.OutboxWriter;
import com.ticketon.ticketon.payment.vo.PaymentConfirmation;
import com.ticketon.ticketon.payment.vo.PaymentMessage;
import com.ticketon.ticketon.payment.vo.PaymentUnknownEvent;
import com.ticketon.ticketon.ticket.dataaccess.entity.Ticket;
import com.ticketon.ticketon.ticket.implement.TicketReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentManager {

    private final PaymentReader paymentReader;
    private final PaymentRepository paymentRepository;
    private final TicketReader ticketReader;
    private final OutboxWriter outboxWriter;

    @Transactional
    public int complete(List<PaymentMessage> messages) {
        List<String> orderIds = messages.stream().map(PaymentMessage::orderId).toList();
        Map<String, Ticket> tickets = ticketReader.findAllByOrderIds(orderIds);
        Map<String, PaymentStatus> existingStatuses = paymentReader.findStatusesByOrderIds(orderIds);

        List<Payment> payments = new ArrayList<>();
        int approvedCount = 0;

        for (PaymentMessage message : messages) {
            Ticket ticket = tickets.get(message.orderId());
            if (ticket == null) {
                log.warn("[결제 완료 처리 : 티켓 없음]: orderId={}", message.orderId());
                continue;
            }
            if (!ticket.isPending()) {
                continue;
            }
            PaymentStatus existingStatus = existingStatuses.get(message.orderId());
            if (existingStatus == null) {
                payments.add(Payment.success(message, ticket.getId(), ticket.getTicketTypeId()));
                ticket.markAsConfirmed();
            } else if (existingStatus == PaymentStatus.UNKNOWN && approveUnknown(message, ticket)) {
                approvedCount++;
                ticket.markAsConfirmed();
            } else {
                log.error("[결제 완료 처리 : 이미 확정된 결제]: orderId={} | status={} | 수동 확인 필요",
                        message.orderId(), existingStatus);
            }
        }
        paymentRepository.saveAll(payments);
        return payments.size() + approvedCount;
    }

    private boolean approveUnknown(PaymentMessage message, Ticket ticket) {
        return paymentRepository.approveUnknown(message.orderId(), ticket.getId(), message.paymentKey(),
                message.amount(), message.approvedAt()) == 1;
    }

    @Transactional
    public void saveUnknown(PaymentConfirmation confirmation, Long ticketTypeId, LocalDateTime requestedAt) {
        paymentRepository.save(Payment.unknown(confirmation, ticketTypeId, requestedAt));
        outboxWriter.savePaymentUnknown(PaymentUnknownEvent.from(confirmation));
    }

    @Transactional
    public boolean failUnknown(String orderId) {
        return paymentRepository.failUnknown(orderId) == 1;
    }

    @Transactional
    public void cancel(String paymentKey, LocalDateTime canceledAt) {
        Payment payment = paymentReader.findByPaymentKey(paymentKey);
        payment.cancel(canceledAt);
    }
}
