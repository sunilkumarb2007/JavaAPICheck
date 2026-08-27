package com.example.payment.dto;

import java.math.BigDecimal;

public class PaymentRequest {
    private String merchantCode;
    private BigDecimal amount;

    public String getMerchantCode() {
        return merchantCode;
    }

    public void setMerchantCode(String merchantCode) {
        this.merchantCode = merchantCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
