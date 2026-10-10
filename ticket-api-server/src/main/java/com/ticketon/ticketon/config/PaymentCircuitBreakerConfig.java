package com.ticketon.ticketon.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentCircuitBreakerConfig {

    private static final String TOSS_CONFIRM = "toss-confirm";
    private static final String TOSS_QUERY = "toss-query";

    @Bean
    public CircuitBreaker tossConfirmCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerRegistry.circuitBreaker(TOSS_CONFIRM);
    }

    @Bean
    public CircuitBreaker tossQueryCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerRegistry.circuitBreaker(TOSS_QUERY);
    }
}
