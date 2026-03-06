package com.farmeazy.dto;

import com.farmeazy.entity.CommunicationPreference;
import com.farmeazy.entity.CommunicationPreference.CommunicationChannel;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * COMMUNICATION PREFERENCE RESPONSE DTO
 * 
 * PURPOSE: Returns user preferences with estimated SMS costs.
 * Helps users understand the cost implications of their choices.
 * 
 * PRICING:
 * - Email: FREE
 * - SMS: ₹0.25 per message
 */
public class CommunicationPreferenceResponseDto {

    private CommunicationChannel otpChannel;
    private CommunicationChannel orderChannel;
    private CommunicationChannel serviceChannel;
    private CommunicationChannel irrigationChannel;
    private CommunicationChannel marketingChannel;
    private Boolean smsConsent;
    private LocalDateTime smsConsentTimestamp;
    
    // Cost estimates
    private Integer estimatedMonthlySmsCount;
    private BigDecimal estimatedMonthlyCost;
    private BigDecimal smsCostPerMessage = new BigDecimal("0.25");

    // Constructors
    public CommunicationPreferenceResponseDto() {}

    public static CommunicationPreferenceResponseDto fromEntity(CommunicationPreference entity) {
        CommunicationPreferenceResponseDto dto = new CommunicationPreferenceResponseDto();
        dto.setOtpChannel(entity.getOtpChannel());
        dto.setOrderChannel(entity.getOrderChannel());
        dto.setServiceChannel(entity.getServiceChannel());
        dto.setIrrigationChannel(entity.getIrrigationChannel());
        dto.setMarketingChannel(entity.getMarketingChannel());
        dto.setSmsConsent(entity.getSmsConsent());
        dto.setSmsConsentTimestamp(entity.getSmsConsentTimestamp());
        dto.setEstimatedMonthlySmsCount(entity.getEstimatedMonthlySmsCount());
        
        // Calculate estimated monthly cost
        int smsCount = dto.calculateEstimatedSmsCount();
        dto.setEstimatedMonthlySmsCount(smsCount);
        dto.setEstimatedMonthlyCost(dto.smsCostPerMessage.multiply(new BigDecimal(smsCount)));
        
        return dto;
    }

    /**
     * Estimate SMS count based on preferences
     * Average usage per category:
     * - OTP: 5 per month (logins, password resets)
     * - Order: 10 per month (order confirmations, payment updates)
     * - Service: 3 per month (bookings, completions)
     * - Irrigation: 15 per month (reminders)
     */
    private int calculateEstimatedSmsCount() {
        if (!Boolean.TRUE.equals(smsConsent)) return 0;
        
        int count = 0;
        if (includesSms(otpChannel)) count += 5;
        if (includesSms(orderChannel)) count += 10;
        if (includesSms(serviceChannel)) count += 3;
        if (includesSms(irrigationChannel)) count += 15;
        // Marketing SMS not allowed
        return count;
    }

    private boolean includesSms(CommunicationChannel channel) {
        return channel == CommunicationChannel.SMS_ONLY || channel == CommunicationChannel.BOTH;
    }

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

    public LocalDateTime getSmsConsentTimestamp() { return smsConsentTimestamp; }
    public void setSmsConsentTimestamp(LocalDateTime smsConsentTimestamp) { this.smsConsentTimestamp = smsConsentTimestamp; }

    public Integer getEstimatedMonthlySmsCount() { return estimatedMonthlySmsCount; }
    public void setEstimatedMonthlySmsCount(Integer estimatedMonthlySmsCount) { this.estimatedMonthlySmsCount = estimatedMonthlySmsCount; }

    public BigDecimal getEstimatedMonthlyCost() { return estimatedMonthlyCost; }
    public void setEstimatedMonthlyCost(BigDecimal estimatedMonthlyCost) { this.estimatedMonthlyCost = estimatedMonthlyCost; }

    public BigDecimal getSmsCostPerMessage() { return smsCostPerMessage; }
}
