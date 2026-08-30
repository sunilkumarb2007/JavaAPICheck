package com.codeguardian.paymentservice;

import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class PaymentService {

    private final MerchantRepository merchantRepository;
    private final PaymentRepository paymentRepository;

    public PaymentService(MerchantRepository merchantRepository, PaymentRepository paymentRepository) {
        this.merchantRepository = merchantRepository;
        this.paymentRepository = paymentRepository;
    }

    public Payment processPayment(CheckoutRequest request) {
        // Find merchant by code
        Merchant merchant = merchantRepository.findByMerchantCode(request.merchantCode());

        // BUG: Missing null check! If merchant is null, dereferencing merchant.isActive() throws NullPointerException
        if (!merchant.isActive()) {
            throw new IllegalStateException("Merchant is not active");
        }

        // Process payment
        String txRef = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Payment payment = new Payment(
                request.orderId(),
                request.merchantCode(),
                request.amount(),
                "SUCCESS",
                txRef
        );
        return paymentRepository.save(payment);
    }
}
