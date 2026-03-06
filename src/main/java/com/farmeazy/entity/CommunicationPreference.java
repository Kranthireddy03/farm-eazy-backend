package com.farmeazy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * COMMUNICATION PREFERENCE ENTITY
 * 
 * PURPOSE: Stores user preferences for how they want to receive notifications.
 * Allows users to choose between Email (free), SMS (₹0.25/msg), or Both.
 * 
 * DESIGN PHILOSOPHY:
 * - Email is FREE and default for all communications
 * - SMS costs ₹0.25 per message, opt-in only
 * - Critical notifications (OTP) can be customized per user
 * - Non-urgent notifications default to email-only
 * 
 * NOTIFICATION CATEGORIES:
 * - OTP_NOTIFICATIONS: Login/Registration OTPs
 * - ORDER_NOTIFICATIONS: Payment success/failure, order updates
 * - SERVICE_NOTIFICATIONS: Service bookings and completions
 * - IRRIGATION_ALERTS: Irrigation reminders
 * - MARKETING: Promotional messages (email-only by default)
 */
@Entity
@Table(name = "communication_preferences")
public class CommunicationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * OTP Notification Preference
     * Default: EMAIL_ONLY (free), user can upgrade to SMS or BOTH
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "otp_channel", nullable = false)
    private CommunicationChannel otpChannel = CommunicationChannel.EMAIL_ONLY;

    /**
     * Order Notification Preference (payment success/failure, confirmations)
     * Default: EMAIL_ONLY
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "order_channel", nullable = false)
    private CommunicationChannel orderChannel = CommunicationChannel.EMAIL_ONLY;

    /**
     * Service Notification Preference (bookings, completions)
     * Default: EMAIL_ONLY
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "service_channel", nullable = false)
    private CommunicationChannel serviceChannel = CommunicationChannel.EMAIL_ONLY;

    /**
     * Irrigation Alert Preference
     * Default: EMAIL_ONLY
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "irrigation_channel", nullable = false)
    private CommunicationChannel irrigationChannel = CommunicationChannel.EMAIL_ONLY;

    /**
     * Marketing/Promotional Notification Preference
     * Default: EMAIL_ONLY (no SMS for marketing)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "marketing_channel", nullable = false)
    private CommunicationChannel marketingChannel = CommunicationChannel.EMAIL_ONLY;

    /**
     * User consent for SMS communications
     * Must be true before any SMS can be sent
     */
    @Column(name = "sms_consent", nullable = false)
    private Boolean smsConsent = false;

    /**
     * User consent timestamp (for compliance)
     */
    @Column(name = "sms_consent_timestamp")
    private LocalDateTime smsConsentTimestamp;

    /**
     * Track estimated SMS cost per month (informational)
     */
    @Column(name = "estimated_monthly_sms_count")
    private Integer estimatedMonthlySmsCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public CommunicationPreference() {}

    public CommunicationPreference(User user) {
        this.user = user;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

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
    public void setSmsConsent(Boolean smsConsent) { 
        this.smsConsent = smsConsent; 
        if (smsConsent) {
            this.smsConsentTimestamp = LocalDateTime.now();
        }
    }

    public LocalDateTime getSmsConsentTimestamp() { return smsConsentTimestamp; }
    public void setSmsConsentTimestamp(LocalDateTime smsConsentTimestamp) { this.smsConsentTimestamp = smsConsentTimestamp; }

    public Integer getEstimatedMonthlySmsCount() { return estimatedMonthlySmsCount; }
    public void setEstimatedMonthlySmsCount(Integer estimatedMonthlySmsCount) { this.estimatedMonthlySmsCount = estimatedMonthlySmsCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Helper methods
    public boolean shouldSendSms(NotificationType type) {
        if (!smsConsent) return false;
        
        CommunicationChannel channel = getChannelForType(type);
        return channel == CommunicationChannel.SMS_ONLY || channel == CommunicationChannel.BOTH;
    }

    public boolean shouldSendEmail(NotificationType type) {
        CommunicationChannel channel = getChannelForType(type);
        return channel == CommunicationChannel.EMAIL_ONLY || channel == CommunicationChannel.BOTH;
    }

    private CommunicationChannel getChannelForType(NotificationType type) {
        return switch (type) {
            case OTP, PASSWORD_RESET -> otpChannel;
            case PAYMENT_SUCCESS, PAYMENT_FAILED, ORDER_UPDATE -> orderChannel;
            case SERVICE_BOOKING, SERVICE_COMPLETED, BOOKING_CANCELLED -> serviceChannel;
            case IRRIGATION_REMINDER -> irrigationChannel;
            case WELCOME, MARKETING -> marketingChannel;
        };
    }

    /**
     * Communication Channel Options
     */
    public enum CommunicationChannel {
        EMAIL_ONLY,    // Free - Default
        SMS_ONLY,      // ₹0.25 per message
        BOTH           // Email (free) + SMS (₹0.25)
    }

    /**
     * Notification Types mapping to MSG91 templates
     */
    public enum NotificationType {
        // OTP Templates
        OTP,                    // FARMEAZY_LOGIN_OTP
        PASSWORD_RESET,         // FARMEAZY_PASSWORD_RESET_OTP
        
        // Order/Payment Templates
        PAYMENT_SUCCESS,        // FARMEAZY_PAYMENT_SUCCESS
        PAYMENT_FAILED,         // FARMEAZY_PAYMENT_FAILED
        ORDER_UPDATE,
        
        // Service Templates
        SERVICE_BOOKING,
        SERVICE_COMPLETED,      // FARMEAZY_SERVICE_COMPLETED
        BOOKING_CANCELLED,      // FARMEAZY_BOOKING_CANCELLED
        
        // Irrigation
        IRRIGATION_REMINDER,    // FARMEAZY_IRRIGATION_REMINDER
        
        // General
        WELCOME,                // FARMEAZY_WELCOME
        MARKETING
    }
}
