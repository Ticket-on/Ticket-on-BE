package com.ticketon.ticketon.ticket.implement;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.ticket.dataaccess.repository.PendingTicketRedisRepository;
import com.ticketon.ticketon.ticket.vo.PendingTicket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PendingTicketManager {

    private final PendingTicketRedisRepository pendingTicketRedisRepository;

    public void save(PendingTicket pendingTicket) {
        pendingTicketRedisRepository.save(pendingTicket);
    }

    public void delete(String orderId) {
        pendingTicketRedisRepository.delete(orderId);
    }

    public PendingTicket getPayable(String orderId) {
        PendingTicket pendingTicket = pendingTicketRedisRepository.find(orderId)
                .orElseThrow(() -> new AppException(ErrorType.PAYMENT_RESERVATION_EXPIRED));
        if (pendingTicketRedisRepository.isUnknown(orderId)) {
            throw new AppException(ErrorType.PAYMENT_RESULT_UNKNOWN);
        }
        return pendingTicket;
    }

    public void markPaid(String orderId) {
        pendingTicketRedisRepository.savePaid(orderId);
    }

    public void markUnknown(String orderId) {
        pendingTicketRedisRepository.saveUnknown(orderId);
    }

    public boolean isPaid(String orderId) {
        return pendingTicketRedisRepository.isPaid(orderId);
    }
}
