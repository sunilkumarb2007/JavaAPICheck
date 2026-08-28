package com.codeguardian.paymentservice;

import org.springframework.stereotype.Service;

@Service
public class PaymentProcessingService {

    private final PaymentService paymentService;

    public PaymentProcessingService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public CheckoutResponse charge(CheckoutRequest request) {
        Payment payment = paymentService.processPayment(request);
        return new CheckoutResponse("SUCCESS", "Payment processed successfully: " + payment.getTransactionRef(), null);
    }
}
