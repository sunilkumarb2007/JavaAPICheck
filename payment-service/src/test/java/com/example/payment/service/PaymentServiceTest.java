package com.example.payment.service;

import com.example.payment.dto.PaymentRequest;
import com.example.payment.model.Merchant;
import com.example.payment.repository.MerchantRepository;
import com.example.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private MerchantRepository merchantRepository;

    @Test
    public void testProcessPayment_Success() {
        Merchant merchant = new Merchant("VALID_MERCHANT", true);
        merchantRepository.save(merchant);

        PaymentRequest request = new PaymentRequest();
        request.setMerchantCode("VALID_MERCHANT");
        request.setAmount(new BigDecimal("100.00"));

        assertNotNull(paymentService.processPayment(request));
    }

    @Test
    public void testProcessPayment_UnknownMerchant_ThrowsNPE() {
        PaymentRequest request = new PaymentRequest();
        request.setMerchantCode("UNKNOWN_MERCHANT");
        request.setAmount(new BigDecimal("50.00"));

        // This demonstrates the deliberate NullPointerException defect
        assertThrows(NullPointerException.class, () -> {
            paymentService.processPayment(request);
        });
    }
}
