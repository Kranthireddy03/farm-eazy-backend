package com.farmeazy.dto;

import com.farmeazy.entity.CommunicationPreference.CommunicationChannel;
import jakarta.validation.constraints.NotNull;

/**
 * COMMUNICATION PREFERENCE DTO
 * 
 * PURPOSE: Transfer user's notification channel preferences.
 * 
 * PRICING MODEL:
 * - EMAIL_ONLY: Free (default)
 * - SMS_ONLY: ₹0.25 per message
 * - BOTH: Email (free) + SMS (₹0.25)
 * 
 * USAGE:
 * - Frontend sends this to update user preferences
 * - Backend validates and stores in CommunicationPreference entity
 */
public class CommunicationPreferenceDto {

    /**
     * OTP notifications (Login, Password Reset)
     * Options: EMAIL_ONLY (free), SMS_ONLY (₹0.25), BOTH
     */
    @NotNull(message = "OTP channel preference is required")
    private CommunicationChannel otpChannel;

    /**
     * Order notifications (Payment Success/Failure)
     * Options: EMAIL_ONLY (free), SMS_ONLY (₹0.25), BOTH
     */
    @NotNull(message = "Order channel preference is required")
    private CommunicationChannel orderChannel;

    /**
     * Service notifications (Bookings, Completions)
     * Options: EMAIL_ONLY (free), SMS_ONLY (₹0.25), BOTH
     */
    @NotNull(message = "Service channel preference is required")
    private CommunicationChannel serviceChannel;

    /**
     * Irrigation alerts
     * Options: EMAIL_ONLY (free), SMS_ONLY (₹0.25), BOTH
     */
    @NotNull(message = "Irrigation channel preference is required")
    private CommunicationChannel irrigationChannel;

    /**
     * Marketing notifications
     * Options: EMAIL_ONLY (free) - SMS not available for marketing
     */
    private CommunicationChannel marketingChannel = CommunicationChannel.EMAIL_ONLY;

    /**
     * User consent for SMS communications
     * Must be true for any SMS to be sent
     */
    @NotNull(message = "SMS consent is required")
    private Boolean smsConsent;

    // Constructors
    public CommunicationPreferenceDto() {}

    // Getters and Setters
    public CommunicationChannel getOtpChannel() { return otpChannel; }
    public void setOtpChannel(CommunicationChannel otpChannel) { this.otpChannel = otpChannel; }

    public CommunicationChannel getOrderChannel() { return orderChannel; }
    public void setOrderChannel(CommunicationChannel orderChannel) { this.orderChannel = orderChannel; }

    public CommunicationChannel getServiceChannel() { return serviceChannel; }
    public void setServiceChannel(CommunicationChannel serviceChannel) { this.serviceChannel = serviceChannel; }

    public CommunicationChannel getIrrigationChannel() { return irrigationChannel; }
    public void setIrrigationChannel(CommunicationChannel irrigationChannel) { this.irrigationChannel = irrigationChannel; }

    public CommunicationChannel getMarketingChannel() { return marketingChannel; }
    public void setMarketingChannel(CommunicationChannel marketingChannel) { this.marketingChannel = marketingChannel; }

    public Boolean getSmsConsent() { return smsConsent; }
    public void setSmsConsent(Boolean smsConsent) { this.smsConsent = smsConsent; }
}
