package com.ticketon.ticketon.payment.implement.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.config.PaymentProperties;
import com.ticketon.ticketon.payment.implement.gateway.dto.TossCancelRequest;
import com.ticketon.ticketon.payment.implement.gateway.dto.TossCancelResponse;
import com.ticketon.ticketon.payment.implement.gateway.dto.TossConfirmRequest;
import com.ticketon.ticketon.payment.implement.gateway.dto.TossConfirmResponse;
import com.ticketon.ticketon.payment.implement.gateway.dto.TossErrorResponse;
import com.ticketon.ticketon.payment.vo.PaymentApproval;
import com.ticketon.ticketon.payment.vo.PaymentCancellation;
import com.ticketon.ticketon.payment.vo.PaymentConfirmation;
import com.ticketon.ticketon.payment.vo.PgError;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Objects;

@Slf4j
@Profile("!loadtest")
@Component
@RequiredArgsConstructor
public class TossPaymentGateway implements PaymentGateway {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String DONE = "DONE";

    private final PaymentProperties paymentProperties;

    private final RestClient restClient;

    private final TossErrorClassifier tossErrorClassifier;

    private final ObjectMapper objectMapper;

    private final CircuitBreaker tossConfirmCircuitBreaker;

    /**
     *  `executeSupplier`
     *   먼저 순차적으로 Supplier로 서킷 검사를 하도록 하기 위한 함수형 인터페이스
     */
    @Override
    public PaymentApproval requestPaymentConfirm(PaymentConfirmation confirmation) {
        try {
            return tossConfirmCircuitBreaker.executeSupplier(() -> confirm(confirmation));
        } catch (CallNotPermittedException e) {
            log.warn("[토스 결제 승인 : 서킷 오픈]: orderId={}", confirmation.orderId());
            throw new AppException(ErrorType.PG_CIRCUIT_OPEN, e);
        }
    }

    private PaymentApproval confirm(PaymentConfirmation confirmation) {
        TossConfirmResponse response;
        try {
            response = restClient.method(HttpMethod.POST)
                    .uri(paymentProperties.confirmUrl())
                    .header(IDEMPOTENCY_KEY_HEADER, confirmation.orderId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(TossConfirmRequest.from(confirmation))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        TossErrorResponse error = readErrorBody(res);
                        log.warn("[토스 결제 승인 : 오류 응답]: orderId={} | status={} | error={}",
                                confirmation.orderId(), res.getStatusCode(), error);
                        throw tossErrorClassifier.classify(res.getStatusCode(), error);
                    })
                    .body(TossConfirmResponse.class);
        } catch (AppException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new AppException(ErrorType.PG_UNAVAILABLE, new PgError("NETWORK_ERROR", e.getMessage()), e);
        } catch (RestClientException e) {
            throw new AppException(ErrorType.PAYMENT_RESULT_UNKNOWN, new PgError("RESPONSE_PARSE_FAILED", e.getMessage()), e);
        }

        validateConfirmed(confirmation, response);
        return response.toApproval();
    }

    private void validateConfirmed(PaymentConfirmation confirmation, TossConfirmResponse response) {
        if (response == null
                || !DONE.equals(response.status())
                || !Objects.equals(response.totalAmount(), confirmation.amount())) {
            log.error("[토스 결제 승인 : 응답 이상]: orderId={} | requestAmount={} | status={} | amount={}",
                    confirmation.orderId(), confirmation.amount(),
                    response != null ? response.status() : null,
                    response != null ? response.totalAmount() : null);
            throw new AppException(ErrorType.PAYMENT_REJECTED, new PgError("INVALID_CONFIRM_RESPONSE", "승인 응답이 요청과 일치하지 않습니다."));
        }
    }

    private TossErrorResponse readErrorBody(ClientHttpResponse res) throws IOException {
        byte[] body = res.getBody().readAllBytes();
        try {
            return objectMapper.readValue(body, TossErrorResponse.class);
        } catch (IOException e) {
            log.warn("[토스 결제 승인 : 에러 바디 파싱 실패]: body={}", new String(body, StandardCharsets.UTF_8));
            return null;
        }
    }

    @Override
    public LocalDateTime requestPaymentCancel(PaymentCancellation cancellation) {
        TossCancelResponse response = restClient.method(HttpMethod.POST)
                .uri(paymentProperties.cancelUrl(cancellation.paymentKey()))
                .header(IDEMPOTENCY_KEY_HEADER, "cancel-" + cancellation.paymentKey() + "-" + cancellation.cancelAmount())
                .contentType(MediaType.APPLICATION_JSON)
                .body(TossCancelRequest.from(cancellation))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    log.warn("[토스 결제 취소 : 오류 응답]: paymentKey={} | status={}",
                            cancellation.paymentKey(), res.getStatusCode());
                    throw new AppException(ErrorType.PAYMENT_CANCEL_FAILED);
                })
                .body(TossCancelResponse.class);

        OffsetDateTime canceledAt = response != null ? response.canceledAt() : null;
        if (canceledAt == null) {
            throw new AppException(ErrorType.PAYMENT_CANCEL_FAILED);
        }
        return canceledAt.toLocalDateTime();
    }
}
