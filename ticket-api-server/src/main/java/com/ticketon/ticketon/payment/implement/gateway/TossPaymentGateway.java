package com.ticketon.ticketon.payment.implement.gateway;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.config.PaymentProperties;
import com.ticketon.ticketon.payment.implement.gateway.dto.TossCancelRequest;
import com.ticketon.ticketon.payment.implement.gateway.dto.TossCancelResponse;
import com.ticketon.ticketon.payment.implement.gateway.dto.TossConfirmRequest;
import com.ticketon.ticketon.payment.implement.gateway.dto.TossConfirmResponse;
import com.ticketon.ticketon.payment.vo.PaymentApproval;
import com.ticketon.ticketon.payment.vo.PaymentCancellation;
import com.ticketon.ticketon.payment.vo.PaymentConfirmation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentGateway implements PaymentGateway {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final PaymentProperties paymentProperties;

    private final RestClient restClient;

    @Override
    public PaymentApproval requestPaymentConfirm(PaymentConfirmation confirmation) {
        TossConfirmResponse response = restClient.method(HttpMethod.POST)
                .uri(paymentProperties.confirmUrl())
                .header(IDEMPOTENCY_KEY_HEADER, confirmation.orderId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(TossConfirmRequest.from(confirmation))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    log.error("[토스 결제 승인 : 오류 응답]: orderId={} | status={} | body={}",
                            confirmation.orderId(), res.getStatusCode(), readBody(res));
                    throw new AppException(ErrorType.PAYMENT_CONFIRM_FAILED);
                })
                .body(TossConfirmResponse.class);

        if (response == null) {
            throw new AppException(ErrorType.PAYMENT_CONFIRM_FAILED);
        }
        return response.toApproval();
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

    private String readBody(ClientHttpResponse res) throws IOException {
        return new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
    }
}
