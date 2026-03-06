package com.farmeazy.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * OTP RESPONSE DTO
 * 
 * PURPOSE: Rich response for OTP send operations.
 * Includes information about which channels were used (Email/SMS)
 * and provides user-friendly messages for frontend display.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtpResponseDto {
    
    // Core status
    private boolean success;
    private String message;           // Technical message
    private String displayMessage;    // User-friendly message for popup
    
    // Delivery channels
    private List<String> sentVia;     // ["Email", "SMS"] - successful channels
    private List<String> failedVia;   // Failed channels (if any)
    
    // SMS details (optional)
    private SmsResponseDto smsResponse;

    // Constructors
    public OtpResponseDto() {}

    public OtpResponseDto(boolean success, String message, String displayMessage) {
        this.success = success;
        this.message = message;
        this.displayMessage = displayMessage;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDisplayMessage() {
        return displayMessage;
    }

    public void setDisplayMessage(String displayMessage) {
        this.displayMessage = displayMessage;
    }

    public List<String> getSentVia() {
        return sentVia;
    }

    public void setSentVia(List<String> sentVia) {
        this.sentVia = sentVia;
    }

    public List<String> getFailedVia() {
        return failedVia;
    }

    public void setFailedVia(List<String> failedVia) {
        this.failedVia = failedVia;
    }

    public SmsResponseDto getSmsResponse() {
        return smsResponse;
    }

    public void setSmsResponse(SmsResponseDto smsResponse) {
        this.smsResponse = smsResponse;
    }
}
