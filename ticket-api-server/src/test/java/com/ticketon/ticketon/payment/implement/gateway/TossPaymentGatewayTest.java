package com.ticketon.ticketon.payment.implement.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.config.PaymentProperties;
import com.ticketon.ticketon.payment.vo.PaymentConfirmation;
import com.ticketon.ticketon.payment.vo.PaymentInquiry;
import com.ticketon.ticketon.payment.vo.PgError;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TossPaymentGatewayTest {

    private static final String CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final String QUERY_URL = "https://api.tosspayments.com/v1/payments/orders/order-1";

    private MockRestServiceServer server;
    private TossPaymentGateway gateway;

    private final PaymentConfirmation confirmation = new PaymentConfirmation("pk", 1L, "order-1", 10000);

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        PaymentProperties properties = new PaymentProperties(
                "secret", "https://api.tosspayments.com", "/v1/payments/confirm", "/v1/payments/%s/cancel",
                "/v1/payments/orders/%s");

        gateway = new TossPaymentGateway(properties, builder.build(), new TossErrorClassifier(),
                new ObjectMapper().findAndRegisterModules(), CircuitBreaker.ofDefaults("confirm"), CircuitBreaker.ofDefaults("query"));
    }

    @Test
    @DisplayName("DONE + 금액 일치면 승인 결과를 반환한다")
    void success() {
        server.expect(requestTo(CONFIRM_URL))
                .andExpect(header("Idempotency-Key", "order-1"))
                .andRespond(withSuccess(successBody("DONE", 10000), MediaType.APPLICATION_JSON));

        assertThat(gateway.requestPaymentConfirm(confirmation).paymentKey()).isEqualTo("pk");
    }

    @Test
    @DisplayName("2xx 인데 금액이 다르면 돈이 빠져나갔을 수 있으므로 PAYMENT_RESULT_UNKNOWN")
    void amountMismatch() {
        server.expect(requestTo(CONFIRM_URL))
                .andRespond(withSuccess(successBody("DONE", 9999), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> gateway.requestPaymentConfirm(confirmation))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorType()).isEqualTo(ErrorType.PAYMENT_RESULT_UNKNOWN));
    }

    @Test
    @DisplayName("read timeout 은 PG_UNAVAILABLE (서킷 집계 대상)")
    void timeout() {
        server.expect(requestTo(CONFIRM_URL))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> gateway.requestPaymentConfirm(confirmation))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorType()).isEqualTo(ErrorType.PG_UNAVAILABLE));
    }

    @Test
    @DisplayName("2xx 응답 파싱 실패는 PAYMENT_RESULT_UNKNOWN (서킷 집계 제외)")
    void parseFailure() {
        server.expect(requestTo(CONFIRM_URL))
                .andRespond(withSuccess("{not-json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> gateway.requestPaymentConfirm(confirmation))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorType()).isEqualTo(ErrorType.PAYMENT_RESULT_UNKNOWN));
    }

    @Test
    @DisplayName("에러 응답은 분류기 결과를 그대로 던진다")
    void errorResponseIsClassified() {
        server.expect(requestTo(CONFIRM_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"REJECT_CARD_PAYMENT\",\"message\":\"한도초과 혹은 잔액부족\"}"));

        assertThatThrownBy(() -> gateway.requestPaymentConfirm(confirmation))
                .isInstanceOfSatisfying(AppException.class, e -> {
                    assertThat(e.getErrorType()).isEqualTo(ErrorType.PAYMENT_REJECTED);
                    assertThat(e.getData()).isEqualTo(new PgError("REJECT_CARD_PAYMENT", "한도초과 혹은 잔액부족"));
                });
    }

    @Test
    @DisplayName("조회 결과를 PaymentInquiry 로 변환한다")
    void inquire() {
        server.expect(requestTo(QUERY_URL))
                .andRespond(withSuccess(successBody("DONE", 10000), MediaType.APPLICATION_JSON));

        PaymentInquiry inquiry = gateway.inquire("order-1");

        assertThat(inquiry.status()).isEqualTo("DONE");
        assertThat(inquiry.totalAmount()).isEqualTo(10000);
        assertThat(inquiry.paymentKey()).isEqualTo("pk");
    }

    @Test
    @DisplayName("조회 404 는 예외가 아니라 NOT_FOUND 상태로 돌려준다")
    void inquireNotFound() {
        server.expect(requestTo(QUERY_URL))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"NOT_FOUND_PAYMENT\",\"message\":\"없음\"}"));

        assertThat(gateway.inquire("order-1")).isEqualTo(PaymentInquiry.notFound("order-1"));
    }

    @Test
    @DisplayName("조회 5xx 는 PG_UNAVAILABLE")
    void inquireServerError() {
        server.expect(requestTo(QUERY_URL))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> gateway.inquire("order-1"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorType()).isEqualTo(ErrorType.PG_UNAVAILABLE));
    }

    @Test
    @DisplayName("조회 응답 파싱 실패는 PAYMENT_RESULT_UNKNOWN (서킷 집계 제외)")
    void inquireParseFailure() {
        server.expect(requestTo(QUERY_URL))
                .andRespond(withSuccess("{not-json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> gateway.inquire("order-1"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorType()).isEqualTo(ErrorType.PAYMENT_RESULT_UNKNOWN));
    }

    private String successBody(String status, int amount) {
        return """
                {"orderId":"order-1","paymentKey":"pk","totalAmount":%d,"status":"%s",
                 "requestedAt":"2025-07-12T14:30:00+09:00","approvedAt":"2025-07-12T14:30:05+09:00"}
                """.formatted(amount, status);
    }
}
