package com.ticketon.ticketon.payment.implement.gateway;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.payment.implement.gateway.dto.TossErrorResponse;
import com.ticketon.ticketon.payment.vo.PgError;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TossErrorClassifier {
    // 실패 확정 코드
    private static final Set<String> CONFIRMED_FAILURE_CODES = Set.of(
            "REJECT_CARD_PAYMENT", "REJECT_ACCOUNT_PAYMENT", "REJECT_CARD_COMPANY", "FDS_ERROR",
            "NOT_AVAILABLE_BANK", "EXCEED_MAX_AUTH_COUNT", "NOT_FOUND_PAYMENT_SESSION", "BELOW_MINIMUM_AMOUNT",
            "INVALID_REQUEST", "NOT_FOUND_PAYMENT", "FORBIDDEN_REQUEST",
            "UNAUTHORIZED_KEY", "INVALID_API_KEY"
    );
    private static final String EXCEED_MAX_PREFIX = "EXCEED_MAX_";

    // 재시도 대상 코드
    private static final Set<String> UNKNOWN_BUSINESS_CODES = Set.of(
            "IDEMPOTENT_REQUEST_PROCESSING", "ALREADY_PROCESSING_REQUEST",
            "ALREADY_PROCESSED_PAYMENT",
            "PROVIDER_ERROR", "FAILED_CARD_COMPANY_RESPONSE"
    );

    public AppException classify(HttpStatusCode status, @Nullable TossErrorResponse error) {
        PgError pgError = PgError.from(error);
        // 결제 실패 확정건
        if (isConfirmedFailure(pgError.code())) {
            return new AppException(ErrorType.PAYMENT_REJECTED, pgError);
        }
        // 
        if (isUnknownBusinessError(pgError.code())) {
            return new AppException(ErrorType.PAYMENT_RESULT_UNKNOWN, pgError);
        }
        if (isPathFailure(status)) {
            return new AppException(ErrorType.PG_UNAVAILABLE, pgError);
        }
        return new AppException(ErrorType.PAYMENT_RESULT_UNKNOWN, pgError);
    }

    private boolean isConfirmedFailure(@Nullable String code) {
        return code != null && (CONFIRMED_FAILURE_CODES.contains(code) || code.startsWith(EXCEED_MAX_PREFIX));
    }

    private boolean isUnknownBusinessError(@Nullable String code) {
        return code != null && UNKNOWN_BUSINESS_CODES.contains(code);
    }

    private boolean isPathFailure(HttpStatusCode status) {
        return status.is5xxServerError() || status.value() == HttpStatus.TOO_MANY_REQUESTS.value();
    }
}
