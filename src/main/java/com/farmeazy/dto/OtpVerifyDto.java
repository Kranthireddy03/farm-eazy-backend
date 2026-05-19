package com.farmeazy.dto;

import jakarta.validation.constraints.*;

public class OtpVerifyDto {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "OTP code is required")
    private String otpCode;
    
    @NotBlank(message = "Purpose is required")
    private String purpose;

    // Optional: phone for phone-based registration
    private String phone;
    
    // Getters and Setters
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getOtpCode() {
        return otpCode;
    }
    
    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }
    
    public String getPurpose() {
        return purpose;
    }
    
    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public OtpVerifyDto() {}

    public OtpVerifyDto(String email, String otpCode, String purpose) {
        this.email = email;
        this.otpCode = otpCode;
        this.purpose = purpose;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
