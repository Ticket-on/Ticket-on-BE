package com.ticketon.ticketon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toss")
public record PaymentProperties(
        String secretKey,
        String baseUrl,
        String confirmEndPoint,
        String cancelEndPoint,
        String queryEndPoint
) {
    public String confirmUrl() {
        return baseUrl + confirmEndPoint;
    }

    public String cancelUrl(String paymentKey) {
        return String.format(baseUrl + cancelEndPoint, paymentKey);
    }

    public String queryUrl(String orderId) {
        return String.format(baseUrl + queryEndPoint, orderId);
    }
}
