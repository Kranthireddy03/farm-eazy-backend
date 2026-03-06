package com.farmeazy.dto;

import jakarta.validation.constraints.*;

/**
 * OTP LOGIN VERIFY DTO
 * 
 * PURPOSE: Verify OTP and login user.
 * User provides phone number and OTP code, system verifies and returns JWT token.
 * 
 * ENDPOINT: POST /api/auth/login/verify-otp
 */
public class OtpLoginVerifyDto {
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;
    
    @NotBlank(message = "OTP code is required")
    @Size(min = 6, max = 6, message = "OTP must be 6 digits")
    private String otpCode;
    
    // Getters and Setters
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getOtpCode() {
        return otpCode;
    }
    
    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }
}
