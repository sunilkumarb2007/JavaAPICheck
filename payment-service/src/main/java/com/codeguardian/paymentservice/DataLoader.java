package com.codeguardian.paymentservice;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final MerchantRepository merchantRepository;

    public DataLoader(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    @Override
    public void run(String... args) {
        if (merchantRepository.count() == 0) {
            // Seed verified active demo merchants
            merchantRepository.save(new Merchant("MCH-5002", "CodeGuardian Verified Merchant", true, "key_live_cg_verified_5002"));
            merchantRepository.save(new Merchant("MCH-5003", "Global SecOps Services Ltd", true, "key_live_secops_5003"));
            // Note: MCH-UNKNOWN (order 5001) is intentionally not seeded to trigger the deterministic failure.
        }
    }
}
