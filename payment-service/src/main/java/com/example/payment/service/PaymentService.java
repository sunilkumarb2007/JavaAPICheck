package com.example.payment.service;

import com.example.payment.dto.PaymentRequest;
import com.example.payment.model.Merchant;
import com.example.payment.model.Payment;
import com.example.payment.repository.MerchantRepository;
import com.example.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class PaymentService {

    private final MerchantRepository merchantRepository;
    private final PaymentRepository paymentRepository;

    @Autowired
    public PaymentService(
            MerchantRepository merchantRepository,
            PaymentRepository paymentRepository) {
        this.merchantRepository = merchantRepository;
        this.paymentRepository = paymentRepository;
    }

    public Payment processPayment(PaymentRequest request) {
        String requestId = request.getRequestId() == null
                ? "req-unknown"
                : request.getRequestId();

        Long orderId = request.getOrderId();

        System.out.println(
                "timestamp=" + Instant.now()
                        + " service=payment-service"
                        + " event_type=payment_processing"
                        + " request_id=" + requestId
                        + " order_id=" + orderId
                        + " status_code=200"
                        + " message=Resolving merchant"
        );

        Merchant merchant;

        if (orderId != null && orderId == 5001L) {

            // INTENTIONAL FAILURE:
            // Keep merchant null so CODEGUARDIAN has a deterministic
            // NULL_OBJECT_ACCESS target.

            System.out.println(
                    "timestamp=" + Instant.now()
                            + " service=payment-service"
                            + " event_type=error"
                            + " request_id=" + requestId
                            + " status_code=500"
                            + " error_code=NULL_OBJECT_ACCESS"
                            + " message=Merchant lookup returned null before validation"
            );

            merchant = null;

        } else if (orderId != null && orderId == 5002L) {

            merchant = new Merchant("M5002", true);

        } else if (orderId != null && orderId == 5003L) {

            System.out.println(
                    "timestamp=" + Instant.now()
                            + " service=payment-service"
                            + " event_type=error"
                            + " request_id=" + requestId
                            + " status_code=500"
                            + " error_code=DATABASE_TIMEOUT"
                            + " message=Database query timed out"
            );

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            throw new RuntimeException("Query execution timeout");

        } else if (orderId != null && orderId == 5004L) {

            throw new RuntimeException("Connection refused");

        } else if (orderId != null && orderId == 5006L) {

            throw new IllegalStateException("Invalid payment state");

        } else {

            merchant = merchantRepository.findByMerchantCode(
                    request.getMerchantCode()
            );
        }

        // INTENTIONAL DEFECT FOR CODEGUARDIAN DEMO.
        // Do not fix this on main.
        if (!merchant.isActive()) {
            throw new RuntimeException("Merchant is not active");
        }

        Payment payment = new Payment(
                request.getMerchantCode(),
                request.getAmount(),
                "SUCCESS"
        );

        return paymentRepository.save(payment);
    }
}
