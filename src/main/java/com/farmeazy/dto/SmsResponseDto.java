package com.farmeazy.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * SMS RESPONSE DTO
 * 
 * PURPOSE: Standardized response for SMS operations.
 * Provides both technical details and user-friendly messages for frontend display.
 * 
 * USAGE:
 * - Backend: Return from SMS service methods
 * - Frontend: Display toast/popup based on success/displayMessage
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SmsResponseDto {
    
    // Core status
    private boolean success;
    private String message;           // Technical message for logging
    private String displayMessage;    // User-friendly message for UI popup
    
    // SMS details
    private String smsType;           // OTP, BOOKING_CONFIRMATION, etc.
    private String phoneNumber;       // Masked phone number
    
    // API response (optional)
    private String apiResponse;       // Raw API response for debugging

    // Constructors
    public SmsResponseDto() {}

    public SmsResponseDto(boolean success, String message, String displayMessage) {
        this.success = success;
        this.message = message;
        this.displayMessage = displayMessage;
    }

    // Static factory methods for common responses
    public static SmsResponseDto success(String smsType, String displayMessage) {
        SmsResponseDto dto = new SmsResponseDto();
        dto.setSuccess(true);
        dto.setSmsType(smsType);
        dto.setMessage("SMS sent successfully");
        dto.setDisplayMessage(displayMessage);
        return dto;
    }

    public static SmsResponseDto failure(String smsType, String reason, String displayMessage) {
        SmsResponseDto dto = new SmsResponseDto();
        dto.setSuccess(false);
        dto.setSmsType(smsType);
        dto.setMessage(reason);
        dto.setDisplayMessage(displayMessage);
        return dto;
    }

    public static SmsResponseDto disabled() {
        SmsResponseDto dto = new SmsResponseDto();
        dto.setSuccess(false);
        dto.setMessage("SMS service disabled");
        dto.setDisplayMessage("SMS notifications are currently disabled");
        return dto;
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

    public String getSmsType() {
        return smsType;
    }

    public void setSmsType(String smsType) {
        this.smsType = smsType;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getApiResponse() {
        return apiResponse;
    }

    public void setApiResponse(String apiResponse) {
        this.apiResponse = apiResponse;
    }

    @Override
    public String toString() {
        return "SmsResponseDto{" +
                "success=" + success +
                ", smsType='" + smsType + '\'' +
                ", message='" + message + '\'' +
                ", displayMessage='" + displayMessage + '\'' +
                '}';
    }
}
