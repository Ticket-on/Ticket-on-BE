package com.ticketon.ticketon.ticket.implement.stock;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.ticket.dataaccess.repository.TicketStockRedisRepository;
import com.ticketon.ticketon.ticket.vo.StockDecreaseResult;
import com.ticketon.ticketon.ticket.vo.TicketOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketStockManager {

    private final TicketStockRedisRepository ticketStockRedisRepository;

    private final Set<Long> soldOutTicketTypeIds = ConcurrentHashMap.newKeySet();

    public void decrease(Long ticketTypeId) {
        if (soldOutTicketTypeIds.contains(ticketTypeId)) {
            throw new AppException(ErrorType.TICKET_SOLD_OUT);
        }

        StockDecreaseResult result = StockDecreaseResult.from(
                ticketStockRedisRepository.decrease(ticketTypeId, TicketOrder.QUANTITY));

        switch (result) {
            case NOT_INITIALIZED -> throw new AppException(ErrorType.TICKET_STOCK_NOT_INITIALIZED, ticketTypeId);
            case SOLD_OUT -> {
                soldOutTicketTypeIds.add(ticketTypeId);
                throw new AppException(ErrorType.TICKET_SOLD_OUT);
            }
            case DECREASED -> { }
        }
    }

    public void restore(Long ticketTypeId) {
        ticketStockRedisRepository.increase(ticketTypeId, TicketOrder.QUANTITY);
        soldOutTicketTypeIds.remove(ticketTypeId);
    }

    public void initialize(Long ticketTypeId, long quantity) {
        ticketStockRedisRepository.set(ticketTypeId, quantity);
        soldOutTicketTypeIds.remove(ticketTypeId);
        log.info("[티켓 재고 : 초기화 완료]: ticketTypeId={} | quantity={}", ticketTypeId, quantity);
    }
}
