package com.farmeazy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public class ApprovePayoutBatchRequest {

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^[0-9]{5,6}$", message = "OTP must be 5-6 digits")
    private String otp;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    public ApprovePayoutBatchRequest() {
    }

    public ApprovePayoutBatchRequest(String otp, String notes) {
        this.otp = otp;
        this.notes = notes;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
