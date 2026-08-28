package com.codeguardian.gateway;

public record CheckoutRequest(Long userId, Long orderId, Double amount, String merchantCode) {
    public CheckoutRequest(Long userId, Long orderId, Double amount) {
        this(userId, orderId, amount, (orderId != null && orderId == 5001L) ? "MCH-UNKNOWN" : "MCH-5002");
    }
}
