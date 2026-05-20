package com.farmeazy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Forgot Password Request DTO
 * Used when user requests password reset
 */
public class ForgotPasswordDto {
    
    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    private String email;

    private String captchaToken;

    public ForgotPasswordDto() {
    }

    public ForgotPasswordDto(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCaptchaToken() {
        return captchaToken;
    }

    public void setCaptchaToken(String captchaToken) {
        this.captchaToken = captchaToken;
    }
}
