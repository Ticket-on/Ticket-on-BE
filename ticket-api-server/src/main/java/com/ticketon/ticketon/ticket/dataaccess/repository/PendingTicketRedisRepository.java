package com.ticketon.ticketon.ticket.dataaccess.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.ticket.vo.PendingTicket;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PendingTicketRedisRepository {

    private static final String TICKET_KEY_PREFIX = "ticket_request:";
    private static final String PAYMENT_KEY_PREFIX = "payment_success:";
    private static final String PAYMENT_UNKNOWN_KEY_PREFIX = "payment_unknown:";
    private static final Duration TTL = Duration.ofMinutes(10L);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(PendingTicket pendingTicket) {
        try {
            String value = objectMapper.writeValueAsString(pendingTicket);
            redisTemplate.opsForValue().set(TICKET_KEY_PREFIX + pendingTicket.orderId(), value, TTL);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorType.PENDING_TICKET_SERIALIZATION_FAILED, e);
        }
    }

    public Optional<PendingTicket> find(String orderId) {
        String value = redisTemplate.opsForValue().get(TICKET_KEY_PREFIX + orderId);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, PendingTicket.class));
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorType.PENDING_TICKET_SERIALIZATION_FAILED, e);
        }
    }

    public void delete(String orderId) {
        redisTemplate.delete(TICKET_KEY_PREFIX + orderId);
    }

    public void savePaid(String orderId) {
        redisTemplate.opsForValue().set(PAYMENT_KEY_PREFIX + orderId, "true", TTL);
    }

    public boolean isPaid(String orderId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PAYMENT_KEY_PREFIX + orderId));
    }

    public void saveUnknown(String orderId) {
        redisTemplate.opsForValue().set(PAYMENT_UNKNOWN_KEY_PREFIX + orderId, "true", TTL);
    }

    public boolean isUnknown(String orderId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PAYMENT_UNKNOWN_KEY_PREFIX + orderId));
    }
}
