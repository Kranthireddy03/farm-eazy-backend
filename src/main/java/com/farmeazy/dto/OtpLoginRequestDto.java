package com.farmeazy.dto;

import jakarta.validation.constraints.*;

/**
 * OTP LOGIN REQUEST DTO
 * 
 * PURPOSE: Request OTP for phone-based login.
 * User provides phone number, system sends LOGIN_OTP via SMS.
 * 
 * ENDPOINT: POST /api/auth/login/request-otp
 */
public class OtpLoginRequestDto {
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    private String captchaToken;
    
    // Getters and Setters
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCaptchaToken() {
        return captchaToken;
    }

    public void setCaptchaToken(String captchaToken) {
        this.captchaToken = captchaToken;
    }
}
