package com.ticketon.ticketon.config;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PaymentCircuitBreakerConfig.class)
@ImportAutoConfiguration(CircuitBreakerAutoConfiguration.class)
class PaymentCircuitBreakerConfigTest {

    @Autowired
    @Qualifier("tossConfirmCircuitBreaker")
    private CircuitBreaker tossConfirmCircuitBreaker;

    @Autowired
    @Qualifier("tossQueryCircuitBreaker")
    private CircuitBreaker tossQueryCircuitBreaker;

    @Test
    @DisplayName("application.yml 의 toss-confirm 설정과 집계 predicate 가 적용된다")
    void bindsTossConfirmConfig() {
        var config = tossConfirmCircuitBreaker.getCircuitBreakerConfig();

        assertThat(tossConfirmCircuitBreaker.getName()).isEqualTo("toss-confirm");
        assertThat(config.getSlidingWindowSize()).isEqualTo(20);
        assertThat(config.getSlowCallDurationThreshold()).isEqualTo(Duration.ofSeconds(3));
        assertThat(config.getRecordExceptionPredicate().test(new AppException(ErrorType.PG_UNAVAILABLE))).isTrue();
        assertThat(config.getRecordExceptionPredicate().test(new AppException(ErrorType.PAYMENT_REJECTED))).isFalse();
    }

    @Test
    @DisplayName("조회 경로는 승인과 다른 toss-query 서킷을 쓴다")
    void bindsTossQueryConfig() {
        assertThat(tossQueryCircuitBreaker.getName()).isEqualTo("toss-query");
        assertThat(tossQueryCircuitBreaker).isNotSameAs(tossConfirmCircuitBreaker);
        assertThat(tossQueryCircuitBreaker.getCircuitBreakerConfig().getRecordExceptionPredicate()
                .test(new AppException(ErrorType.PG_UNAVAILABLE))).isTrue();
    }
}
