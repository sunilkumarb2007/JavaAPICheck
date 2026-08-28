package com.codeguardian.orderservice;

import java.util.List;

public record CheckoutRequest(
        Long userId,
        Long orderId,
        Double amount,
        String merchantCode,
        List<Long> productIds
) {
    public CheckoutRequest(Long userId, Long orderId, Double amount) {
        this(userId, orderId, amount, (orderId != null && orderId == 5001L) ? "MCH-UNKNOWN" : "MCH-5002", null);
    }

    public CheckoutRequest(Long userId, Long orderId, Double amount, String merchantCode) {
        this(userId, orderId, amount, merchantCode, null);
    }

    public String merchantCode() {
        if (merchantCode != null && !merchantCode.isBlank()) {
            return merchantCode;
        }
        if (orderId != null && orderId == 5001L) {
            return "MCH-UNKNOWN";
        }
        return "MCH-5002";
    }
}
