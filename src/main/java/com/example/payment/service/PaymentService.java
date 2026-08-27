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
        // Find merchant by code
        Merchant merchant = merchantRepository.findByMerchantCode(request.getMerchantCode());

        // INTENTIONAL DEFECT: Assuming merchant is always found and not null.
        // If findByMerchantCode returns null (merchant does not exist), 
        // the next line will throw a NullPointerException.
        if (!merchant.isActive()) {
            throw new RuntimeException("Merchant is not active");
        }

        // Process payment
        Payment payment = new Payment(request.getMerchantCode(), request.getAmount(), "SUCCESS");
        return paymentRepository.save(payment);
    }
}
