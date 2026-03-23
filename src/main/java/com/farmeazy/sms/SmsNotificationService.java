package com.farmeazy.sms;

import com.farmeazy.dto.SmsResponseDto;
import com.farmeazy.entity.CommunicationPreference;
import com.farmeazy.entity.CommunicationPreference.CommunicationChannel;
import com.farmeazy.entity.CommunicationPreference.NotificationType;
import com.farmeazy.entity.User;
import com.farmeazy.repository.CommunicationPreferenceRepository;
import com.farmeazy.repository.UserRepository;
import com.farmeazy.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * SMS Notification Service
 * 
 * Handles SMS notifications with user preference checking.
 * Respects user's communication channel settings and SMS consent.
 */
@Service
public class SmsNotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(SmsNotificationService.class);
    
    @Autowired
    private SmsService smsService;
    
    @Autowired
    private CommunicationPreferenceRepository preferenceRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Check if SMS should be sent based on user preferences
     */
    public boolean shouldSendSms(Long userId, NotificationType notificationType) {
        Optional<CommunicationPreference> prefOpt = preferenceRepository.findByUserId(userId);
        
        if (prefOpt.isEmpty()) {
            logger.debug("No communication preferences found for user {}. Defaulting to send SMS (both channels).", userId);
            return true;
        }
        
        CommunicationPreference pref = prefOpt.get();
        
        // Check SMS consent first
        if (!Boolean.TRUE.equals(pref.getSmsConsent())) {
            logger.debug("User {} has not given SMS consent.", userId);
            return false;
        }
        
        // Get channel preference for this notification type
        CommunicationChannel channel = getChannelForNotificationType(pref, notificationType);
        
        return channel == CommunicationChannel.SMS_ONLY || channel == CommunicationChannel.BOTH;
    }
    
    /**
     * Get the channel preference for a notification type
     */
    private CommunicationChannel getChannelForNotificationType(CommunicationPreference pref, NotificationType type) {
        switch (type) {
            case OTP:
            case PASSWORD_RESET:
                return pref.getOtpChannel();
            
            case PAYMENT_SUCCESS:
            case PAYMENT_FAILED:
            case ORDER_UPDATE:
                return pref.getOrderChannel();
            
            case SERVICE_BOOKING:
            case SERVICE_COMPLETED:
            case BOOKING_CANCELLED:
                return pref.getServiceChannel();
            
            case IRRIGATION_REMINDER:
                return pref.getIrrigationChannel();
            
            default:
                return CommunicationChannel.EMAIL_ONLY;
        }
    }
    
    // ===== OTP Notifications =====
    
    /**
     * Send login OTP SMS if user has opted for SMS
     */
    public boolean sendLoginOtp(Long userId, String phoneNumber, String otp, int validityMinutes) {
        if (!shouldSendSms(userId, NotificationType.OTP)) {
            logger.info("User {} has not opted for SMS OTP notifications", userId);
            return false;
        }
        SmsResponseDto response = smsService.sendOtp(phoneNumber, otp, String.valueOf(validityMinutes));
        return response.isSuccess();
    }
    
    /**
     * Send password reset OTP SMS if user has opted for SMS
     */
    public boolean sendPasswordResetOtp(Long userId, String phoneNumber, String otp, int validityMinutes) {
        if (!shouldSendSms(userId, NotificationType.PASSWORD_RESET)) {
            logger.info("User {} has not opted for SMS OTP notifications", userId);
            return false;
        }
        SmsResponseDto response = smsService.sendPasswordResetOtp(phoneNumber, otp, String.valueOf(validityMinutes));
        return response.isSuccess();
    }
    
    /**
     * Send login OTP by email lookup
     */
    public boolean sendLoginOtpByEmail(String email, String otp, int validityMinutes) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            logger.warn("Cannot send OTP SMS - user not found: {}", email);
            return false;
        }
        
        User user = userOpt.get();
        if (user.getPhone() == null || user.getPhone().isEmpty()) {
            logger.warn("Cannot send OTP SMS - no phone number for user: {}", email);
            return false;
        }
        
        return sendLoginOtp(user.getId(), user.getPhone(), otp, validityMinutes);
    }
    
    // ===== Welcome Notification =====
    
    /**
     * Send welcome SMS after registration
     */
    public boolean sendWelcomeSms(Long userId, String phoneNumber, String userName) {
        // Welcome SMS is always sent if SMS is configured and user provided phone
        // This is sent before preferences are set, so we send it by default
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }
        SmsResponseDto response = smsService.sendWelcome(phoneNumber, userName);
        return response.isSuccess();
    }
    
    // ===== Order/Payment Notifications =====
    
    /**
     * Send payment success SMS if user has opted for SMS order notifications
     */
    public boolean sendPaymentSuccess(Long userId, String phoneNumber, String userName, String amount, String orderId) {
        if (!shouldSendSms(userId, NotificationType.PAYMENT_SUCCESS)) {
            logger.info("User {} has not opted for SMS order notifications", userId);
            return false;
        }
        SmsResponseDto response = smsService.sendPaymentSuccess(phoneNumber, userName, amount, orderId);
        return response.isSuccess();
    }
    
    /**
     * Send payment failed SMS if user has opted for SMS order notifications
     */
    public boolean sendPaymentFailed(Long userId, String phoneNumber, String userName, String orderId) {
        if (!shouldSendSms(userId, NotificationType.PAYMENT_FAILED)) {
            logger.info("User {} has not opted for SMS order notifications", userId);
            return false;
        }
        SmsResponseDto response = smsService.sendPaymentFailed(phoneNumber, userName, orderId);
        return response.isSuccess();
    }
    
    // ===== Service Notifications =====
    
    /**
     * Send booking cancelled SMS if user has opted for SMS service notifications
     */
    public boolean sendBookingCancelled(Long userId, String phoneNumber, String userName, String bookingId) {
        if (!shouldSendSms(userId, NotificationType.BOOKING_CANCELLED)) {
            logger.info("User {} has not opted for SMS service notifications", userId);
            return false;
        }
        SmsResponseDto response = smsService.sendBookingCancelled(phoneNumber, userName, bookingId);
        return response.isSuccess();
    }
    
    /**
     * Send service completed SMS if user has opted for SMS service notifications
     */
    public boolean sendServiceCompleted(Long userId, String phoneNumber, String userName, String serviceId) {
        if (!shouldSendSms(userId, NotificationType.SERVICE_COMPLETED)) {
            logger.info("User {} has not opted for SMS service notifications", userId);
            return false;
        }
        SmsResponseDto response = smsService.sendServiceCompleted(phoneNumber, userName, serviceId);
        return response.isSuccess();
    }
    
    // ===== Irrigation Notifications =====
    
    /**
     * Send irrigation reminder SMS if user has opted for SMS irrigation alerts
     */
    public boolean sendIrrigationReminder(Long userId, String phoneNumber, String irrigationId, String farmName) {
        if (!shouldSendSms(userId, NotificationType.IRRIGATION_REMINDER)) {
            logger.info("User {} has not opted for SMS irrigation alerts", userId);
            return false;
        }
        SmsResponseDto response = smsService.sendIrrigationReminder(phoneNumber, irrigationId, farmName);
        return response.isSuccess();
    }
    
    // ===== Status Methods =====
    
    /**
     * Check if SMS service is configured and ready
     */
    public boolean isSmsServiceAvailable() {
        return smsService.isConfigured();
    }
    
    /**
     * Get user's SMS preference for a notification type
     */
    public String getUserSmsPreference(Long userId, NotificationType notificationType) {
        Optional<CommunicationPreference> prefOpt = preferenceRepository.findByUserId(userId);
        if (prefOpt.isEmpty()) {
            return "EMAIL_ONLY";
        }
        return getChannelForNotificationType(prefOpt.get(), notificationType).name();
    }
}
