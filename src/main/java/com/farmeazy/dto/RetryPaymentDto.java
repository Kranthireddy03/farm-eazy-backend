package com.farmeazy.dto;

import lombok.Data;

@Data
public class RetryPaymentDto {
    private String paymentId;
    private String paymentMethod; // Should be 'RAZORPAY'

    public String getPaymentId() {
        return paymentId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }
}
