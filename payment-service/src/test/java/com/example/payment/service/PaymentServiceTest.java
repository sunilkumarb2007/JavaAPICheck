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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
    public void testProcessPayment_UnknownMerchant() {
        PaymentRequest request = new PaymentRequest();
        request.setMerchantCode("UNKNOWN_MERCHANT");
        request.setAmount(new BigDecimal("50.00"));

        try {
            paymentService.processPayment(request);
            org.junit.jupiter.api.Assertions.fail("Expected an exception");
        } catch (NullPointerException e) {
            org.junit.jupiter.api.Assertions.fail("Defect is not patched yet: NullPointerException was thrown");
        } catch (Exception e) {
            // gracefully handled (e.g. IllegalArgumentException, ResponseStatusException)
        }
    }
}
