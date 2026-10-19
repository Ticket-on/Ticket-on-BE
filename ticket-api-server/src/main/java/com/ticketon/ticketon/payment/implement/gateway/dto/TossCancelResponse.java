package com.ticketon.ticketon.payment.implement.gateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossCancelResponse(List<TossCancelDetail> cancels) {

    public OffsetDateTime canceledAt() {
        if (cancels != null && !cancels.isEmpty()) {
            return cancels.get(0).canceledAt();
        }
        return null;
    }
}
