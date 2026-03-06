package com.farmeazy.dto;

import lombok.Data;

@Data
public class RetryPaymentDto {
    private String paymentId;
    private String paymentMethod; // Should be 'RAZORPAY'
    private String amount;

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }
}
