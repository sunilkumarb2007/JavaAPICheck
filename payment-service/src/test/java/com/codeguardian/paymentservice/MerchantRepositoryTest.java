package com.codeguardian.paymentservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MerchantRepositoryTest {

    @Autowired
    private MerchantRepository merchantRepository;

    @Test
    void findByMerchantCodeReturnsEntityWhenExists() {
        Merchant merchant = new Merchant("MCH-TEST-1", "Test Merchant", true, "key_1");
        merchantRepository.save(merchant);

        Merchant found = merchantRepository.findByMerchantCode("MCH-TEST-1");
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Test Merchant");
        assertThat(found.isActive()).isTrue();
    }

    @Test
    void findByMerchantCodeReturnsNullWhenMissing() {
        Merchant found = merchantRepository.findByMerchantCode("MCH-NONEXISTENT");
        assertThat(found).isNull();
    }
}
