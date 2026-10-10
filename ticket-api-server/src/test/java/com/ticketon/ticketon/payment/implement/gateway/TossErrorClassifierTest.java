package com.ticketon.ticketon.payment.implement.gateway;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.payment.implement.gateway.dto.TossErrorResponse;
import com.ticketon.ticketon.payment.vo.PgError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatusCode;

import static org.assertj.core.api.Assertions.assertThat;

class TossErrorClassifierTest {

    private final TossErrorClassifier classifier = new TossErrorClassifier();

    @ParameterizedTest(name = "{0} {1} → {2}")
    @CsvSource({
            "400, REJECT_CARD_PAYMENT,             PAYMENT_REJECTED",
            "403, REJECT_CARD_COMPANY,             PAYMENT_REJECTED",
            "400, EXCEED_MAX_DAILY_PAYMENT_COUNT,  PAYMENT_REJECTED",
            "404, NOT_FOUND_PAYMENT_SESSION,       PAYMENT_REJECTED",
            "400, INVALID_REQUEST,                 PAYMENT_REJECTED",
            "401, UNAUTHORIZED_KEY,                PAYMENT_REJECTED",
            "400, INVALID_API_KEY,                 PAYMENT_REJECTED",
            "409, IDEMPOTENT_REQUEST_PROCESSING,   PAYMENT_RESULT_UNKNOWN",
            "400, ALREADY_PROCESSING_REQUEST,      PAYMENT_RESULT_UNKNOWN",
            "400, ALREADY_PROCESSED_PAYMENT,       PAYMENT_RESULT_UNKNOWN",
            "400, PROVIDER_ERROR,                  PAYMENT_RESULT_UNKNOWN",
            "500, FAILED_CARD_COMPANY_RESPONSE,    PAYMENT_RESULT_UNKNOWN",
            "400, SOME_NEW_CODE,                   PAYMENT_RESULT_UNKNOWN",
    })
    @DisplayName("에러 code 로 ErrorType 을 정하고 토스 원본 code 를 data 에 담는다")
    void classifyByCode(int status, String code, ErrorType expected) {
        AppException e = classifier.classify(HttpStatusCode.valueOf(status), new TossErrorResponse(code, "msg"));

        assertThat(e.getErrorType()).isEqualTo(expected);
        assertThat(e.getData()).isEqualTo(new PgError(code, "msg"));
    }

    @ParameterizedTest(name = "{0} {1} → 서킷 집계 제외")
    @CsvSource({
            "400, REJECT_CARD_PAYMENT",
            "401, UNAUTHORIZED_KEY",
            "400, ALREADY_PROCESSED_PAYMENT",
            "500, PROVIDER_ERROR",
            "500, FAILED_CARD_COMPANY_RESPONSE",
    })
    @DisplayName("토스가 정상 응답한 비즈니스 오류는 status 가 5xx 여도 PG_UNAVAILABLE 이 아니다")
    void businessErrorIsNotPathFailure(int status, String code) {
        AppException e = classifier.classify(HttpStatusCode.valueOf(status), new TossErrorResponse(code, "msg"));

        assertThat(e.getErrorType()).isNotEqualTo(ErrorType.PG_UNAVAILABLE);
    }

    @ParameterizedTest(name = "{0} → PG_UNAVAILABLE")
    @CsvSource({"500", "502", "503", "504", "429"})
    @DisplayName("5xx, 429 는 경로 이상으로 보고 서킷에 집계한다")
    void pathFailure(int status) {
        AppException e = classifier.classify(HttpStatusCode.valueOf(status), new TossErrorResponse("UNKNOWN_PAYMENT_ERROR", "msg"));

        assertThat(e.getErrorType()).isEqualTo(ErrorType.PG_UNAVAILABLE);
    }

    @Test
    @DisplayName("에러 바디를 파싱하지 못하면 PAYMENT_RESULT_UNKNOWN")
    void nullBodyIsUnknown() {
        AppException e = classifier.classify(HttpStatusCode.valueOf(400), null);

        assertThat(e.getErrorType()).isEqualTo(ErrorType.PAYMENT_RESULT_UNKNOWN);
    }

    @Test
    @DisplayName("바디가 없어도 5xx 면 PG_UNAVAILABLE")
    void nullBodyWith5xxIsUnavailable() {
        AppException e = classifier.classify(HttpStatusCode.valueOf(503), null);

        assertThat(e.getErrorType()).isEqualTo(ErrorType.PG_UNAVAILABLE);
    }
}
