package com.farmeazy.dto;

public class PaymentRequestDto {
    private double amount;
    private String email;
    private String phone;
    // Add other fields as needed

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
