package com.ticketon.ticketon.payment.implement.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.config.PaymentProperties;
import com.ticketon.ticketon.payment.vo.PaymentConfirmation;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class TossPaymentGatewayCircuitBreakerTest {

    private static final String CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final int WINDOW = 4;

    private final PaymentConfirmation confirmation = new PaymentConfirmation("pk", 1L, "order-1", 10000);

    private MockRestServiceServer server;
    private CircuitBreaker circuitBreaker;
    private TossPaymentGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();

        circuitBreaker = CircuitBreaker.of("toss-confirm", CircuitBreakerConfig.custom()
                .slidingWindowSize(WINDOW)
                .minimumNumberOfCalls(WINDOW)
                .failureRateThreshold(50)
                .recordException(new PgUnavailablePredicate())
                .build());

        PaymentProperties properties = new PaymentProperties(
                "secret", "https://api.tosspayments.com", "/v1/payments/confirm", "/v1/payments/%s/cancel",
                "/v1/payments/orders/%s");
        gateway = new TossPaymentGateway(properties, builder.build(), new TossErrorClassifier(),
                new ObjectMapper().findAndRegisterModules(), circuitBreaker, CircuitBreaker.ofDefaults("query"));
    }

    @Test
    @DisplayName("PG_UNAVAILABLE 이 쌓이면 서킷이 열리고, 이후 요청은 토스로 보내지 않고 PG_CIRCUIT_OPEN")
    void opensOnPathFailure() {
        server.expect(ExpectedCount.times(WINDOW), requestTo(CONFIRM_URL))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        for (int i = 0; i < WINDOW; i++) {
            confirmIgnoringError();
        }

        AppException e = catchThrowableOfType(AppException.class, () -> gateway.requestPaymentConfirm(confirmation));

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(e.getErrorType()).isEqualTo(ErrorType.PG_CIRCUIT_OPEN);
        server.verify();
    }

    @Test
    @DisplayName("카드 거절처럼 토스가 정상 응답한 실패는 집계하지 않는다")
    void staysClosedOnBusinessFailure() {
        server.expect(ExpectedCount.times(WINDOW), requestTo(CONFIRM_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"REJECT_CARD_PAYMENT\",\"message\":\"거절\"}"));

        for (int i = 0; i < WINDOW; i++) {
            confirmIgnoringError();
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("5xx 라도 PROVIDER_ERROR 같은 비즈니스 code 는 집계하지 않는다")
    void staysClosedOnProviderError() {
        server.expect(ExpectedCount.times(WINDOW), requestTo(CONFIRM_URL))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"PROVIDER_ERROR\",\"message\":\"카드사 오류\"}"));

        for (int i = 0; i < WINDOW; i++) {
            confirmIgnoringError();
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    private void confirmIgnoringError() {
        catchThrowableOfType(AppException.class, () -> gateway.requestPaymentConfirm(confirmation));
    }
}
