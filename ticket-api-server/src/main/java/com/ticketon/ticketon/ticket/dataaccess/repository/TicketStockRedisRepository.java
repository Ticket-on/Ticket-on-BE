package com.ticketon.ticketon.ticket.dataaccess.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TicketStockRedisRepository {

    private static final String STOCK_KEY_PREFIX = "issued_quantity:";

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> decreaseStockScript;

    public Long decrease(Long ticketTypeId, int quantity) {
        return redisTemplate.execute(decreaseStockScript, List.of(stockKey(ticketTypeId)), String.valueOf(quantity));
    }

    public void increase(Long ticketTypeId, int quantity) {
        redisTemplate.opsForValue().increment(stockKey(ticketTypeId), quantity);
    }

    public void set(Long ticketTypeId, long quantity) {
        redisTemplate.opsForValue().set(stockKey(ticketTypeId), String.valueOf(quantity));
    }

    private String stockKey(Long ticketTypeId) {
        return STOCK_KEY_PREFIX + ticketTypeId;
    }
}
