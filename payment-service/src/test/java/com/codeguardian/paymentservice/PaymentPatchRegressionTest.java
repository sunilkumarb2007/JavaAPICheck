package com.codeguardian.paymentservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PaymentPatchRegressionTest {

    @Autowired
    private PaymentProcessingService paymentProcessingService;

    @Test
    void knownBugOrderShouldReturnSuccessAfterPatch() {
        CheckoutResponse response = paymentProcessingService.charge(new CheckoutRequest(101L, 5002L, 499.0));
        assertThat(response.status()).isEqualTo("SUCCESS");
    }
}
