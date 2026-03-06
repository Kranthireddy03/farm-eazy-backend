package com.farmeazy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * SMS CONFIGURATION
 * 
 * PURPOSE: Centralized SMS configuration properties.
 * All MSG91 settings are loaded from application.properties (local)
 * or environment variables (UAT/Prod).
 * 
 * ENVIRONMENT VARIABLE MAPPING:
 * - MSG91_AUTH_KEY -> msg91.authKey
 * - MSG91_SENDER_ID -> msg91.senderId
 * - MSG91_ENABLED -> msg91.enabled
 * - MSG91_TEMPLATE_OTP -> msg91.template.otp
 * - MSG91_TEMPLATE_BOOKING -> msg91.template.booking
 * - MSG91_TEMPLATE_SERVICE_STARTED -> msg91.template.service.started
 * - MSG91_TEMPLATE_SERVICE_COMPLETED -> msg91.template.service.completed
 * - MSG91_TEMPLATE_IRRIGATION -> msg91.template.irrigation
 * - MSG91_TEMPLATE_PAYMENT_SUCCESS -> msg91.template.payment.success
 * - MSG91_TEMPLATE_PAYMENT_FAILED -> msg91.template.payment.failed
 */
@Configuration
public class SmsConfig {

    @Value("${msg91.authKey:}")
    private String authKey;

    @Value("${msg91.senderId:FRMZOT}")
    private String senderId;

    @Value("${msg91.enabled:false}")
    private boolean enabled;

    // Template IDs
    @Value("${msg91.template.otp:}")
    private String otpTemplateId;

    @Value("${msg91.template.booking:}")
    private String bookingTemplateId;

    @Value("${msg91.template.service.started:}")
    private String serviceStartedTemplateId;

    @Value("${msg91.template.service.completed:}")
    private String serviceCompletedTemplateId;

    @Value("${msg91.template.irrigation:}")
    private String irrigationTemplateId;

    @Value("${msg91.template.payment.success:}")
    private String paymentSuccessTemplateId;

    @Value("${msg91.template.payment.failed:}")
    private String paymentFailedTemplateId;

    // Getters
    public String getAuthKey() {
        return authKey;
    }

    public String getSenderId() {
        return senderId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getOtpTemplateId() {
        return otpTemplateId;
    }

    public String getBookingTemplateId() {
        return bookingTemplateId;
    }

    public String getServiceStartedTemplateId() {
        return serviceStartedTemplateId;
    }

    public String getServiceCompletedTemplateId() {
        return serviceCompletedTemplateId;
    }

    public String getIrrigationTemplateId() {
        return irrigationTemplateId;
    }

    public String getPaymentSuccessTemplateId() {
        return paymentSuccessTemplateId;
    }

    public String getPaymentFailedTemplateId() {
        return paymentFailedTemplateId;
    }

    /**
     * Check if SMS is fully configured
     */
    public boolean isConfigured() {
        return enabled && authKey != null && !authKey.isBlank() 
               && !authKey.equals("your-local-msg91-auth-key");
    }

    /**
     * Get configuration summary for logging
     */
    public String getConfigurationSummary() {
        return String.format("SMS Config: enabled=%s, authKey=%s, senderId=%s",
            enabled,
            authKey != null && !authKey.isBlank() ? "SET" : "NOT_SET",
            senderId);
    }
}
