package com.example.payment.service;

import com.example.payment.dto.PaymentRequest;
import com.example.payment.model.Merchant;
import com.example.payment.model.Payment;
import com.example.payment.repository.MerchantRepository;
import com.example.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final MerchantRepository merchantRepository;
    private final PaymentRepository paymentRepository;

    @Autowired
    public PaymentService(MerchantRepository merchantRepository, PaymentRepository paymentRepository) {
        this.merchantRepository = merchantRepository;
        this.paymentRepository = paymentRepository;
    }

    public Payment processPayment(PaymentRequest request) {
        String requestId = request.getRequestId();
        if (requestId == null) requestId = "req-unknown";
        
        System.out.println("timestamp=" + java.time.Instant.now() + " service=payment-service event_type=payment_processing request_id=" + requestId + " order_id=" + request.getOrderId() + " status_code=200 message=Attempting to resolve demo payment record");
        
        Merchant merchant = null;
        
        if (request.getOrderId() != null) {
            long oid = request.getOrderId();
            if (oid == 5001) {
                // FAILURE-001: NULL_OBJECT_ACCESS
                System.out.println("timestamp=" + java.time.Instant.now() + " service=payment-service event_type=error request_id=" + requestId + " status_code=500 error_code=NULL_OBJECT_ACCESS message=Payment request object was null before validation");
                // Deliberately leave merchant null
            } else if (oid == 5002) {
                // SUCCESS
                merchant = new Merchant("M5002", "Demo Merchant");
                merchant.setActive(true);
            } else if (oid == 5003) {
                // FAILURE-002: DATABASE_TIMEOUT
                System.out.println("timestamp=" + java.time.Instant.now() + " service=payment-service event_type=error request_id=" + requestId + " status_code=500 error_code=DATABASE_TIMEOUT message=Database query timed out");
                try { Thread.sleep(5000); } catch (Exception e) {}
                throw new RuntimeException("Query execution timeout");
            } else if (oid == 5004) {
                // FAILURE-003: CONNECTION_REFUSED
                System.out.println("timestamp=" + java.time.Instant.now() + " service=payment-service event_type=error request_id=" + requestId + " status_code=500 error_code=CONNECTION_REFUSED message=Could not connect to external provider");
                throw new java.net.ConnectException("Connection refused (Connection refused)");
            } else if (oid == 5006) {
                // FAILURE-004: INVALID_PAYMENT_STATE
                System.out.println("timestamp=" + java.time.Instant.now() + " service=payment-service event_type=error request_id=" + requestId + " status_code=400 error_code=INVALID_PAYMENT_STATE message=Transition to COMPLETED not allowed from FAILED");
                throw new IllegalStateException("Invalid state transition");
            } else {
                merchant = merchantRepository.findByMerchantCode(request.getMerchantCode());
            }
        } else {
            merchant = merchantRepository.findByMerchantCode(request.getMerchantCode());
        }

        // INTENTIONAL DEFECT: Assuming merchant is always found and not null.
        if (!merchant.isActive()) {
            throw new RuntimeException("Merchant is not active");
        }

        // Process payment
        Payment payment = new Payment(request.getMerchantCode(), request.getAmount(), "SUCCESS");
        return paymentRepository.save(payment);
    }
}
