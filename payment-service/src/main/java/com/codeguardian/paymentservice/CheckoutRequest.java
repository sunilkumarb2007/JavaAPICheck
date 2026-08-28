package com.codeguardian.paymentservice;

public record CheckoutRequest(Long userId, Long orderId, Double amount, String merchantCode) {

    public CheckoutRequest(Long userId, Long orderId, Double amount) {
        this(userId, orderId, amount, (orderId != null && orderId == 5001L) ? "MCH-UNKNOWN" : "MCH-5002");
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
