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
        return pendingTicketRedisRepository.find(orderId)
                .orElseThrow(() -> new AppException(ErrorType.PAYMENT_RESERVATION_EXPIRED));
    }

    public void markPaid(String orderId) {
        pendingTicketRedisRepository.savePaid(orderId);
    }

    public boolean isPaid(String orderId) {
        return pendingTicketRedisRepository.isPaid(orderId);
    }
}
