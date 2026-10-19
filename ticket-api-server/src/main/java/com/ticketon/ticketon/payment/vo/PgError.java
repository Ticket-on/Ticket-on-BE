package com.ticketon.ticketon.payment.vo;

import com.ticketon.ticketon.payment.implement.gateway.dto.TossErrorResponse;

public record PgError(String code, String message) {

    public static PgError from(TossErrorResponse error) {
        if (error == null) {
            return new PgError(null, null);
        }
        return new PgError(error.code(), error.message());
    }
}
