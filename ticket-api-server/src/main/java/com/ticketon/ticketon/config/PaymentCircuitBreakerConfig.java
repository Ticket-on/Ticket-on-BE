package com.ticketon.ticketon.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentCircuitBreakerConfig {

    private static final String TOSS_CONFIRM = "toss-confirm";

    @Bean
    public CircuitBreaker tossConfirmCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerRegistry.circuitBreaker(TOSS_CONFIRM);
    }
}
