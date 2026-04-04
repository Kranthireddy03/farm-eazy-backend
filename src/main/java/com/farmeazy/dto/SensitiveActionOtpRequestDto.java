package com.farmeazy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SensitiveActionOtpRequestDto {

    @NotBlank(message = "Action is required")
    @Pattern(regexp = "ADD|UPDATE|DELETE", message = "Action must be ADD, UPDATE or DELETE")
    private String action;

    private String otpCode;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }
}
