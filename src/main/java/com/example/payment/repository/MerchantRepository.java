package com.example.payment.repository;

import com.example.payment.model.Merchant;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantRepository extends CrudRepository<Merchant, Long> {
    Merchant findByMerchantCode(String merchantCode);
}
