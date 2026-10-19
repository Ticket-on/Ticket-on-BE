package com.ticketon.ticketon.payment.implement.gateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossCancelDetail(OffsetDateTime canceledAt, String cancelReason, int cancelAmount) {
}
