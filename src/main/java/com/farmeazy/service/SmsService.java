package com.farmeazy.service;

import com.farmeazy.dto.SmsResponseDto;
import com.farmeazy.sms.SmsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import com.farmeazy.dto.CommunicationPreferenceResponseDto;
import com.farmeazy.entity.CommunicationPreference;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SMS SERVICE - FarmEazy SMS Communication Hub
 * 
 * PURPOSE: Central service for all SMS communications via MSG91.
 * 
 * SUPPORTED SMS TYPES:
 * 1. OTP - Authentication/Verification
 * 2. Booking Confirmation - Order placed
 * 3. Service Started - Service in progress
 * 4. Service Completed - Service finished
 * 5. Irrigation Reminder - Scheduled irrigation alerts
 * 6. Payment Success - Payment confirmation
 * 7. Payment Failed - Payment failure notification
 * 
 * MSG91 TEMPLATE IDs (configure in application.properties):
 * - msg91.template.otp - OTP template
 * - msg91.template.booking - Booking confirmation
 * - msg91.template.service.started - Service started
 * - msg91.template.service.completed - Service completed
 * - msg91.template.irrigation - Irrigation reminder
 * - msg91.template.payment.success - Payment success
 * - msg91.template.payment.failed - Payment failed
 * - msg91.template.password.reset - Password reset OTP
 * - msg91.template.welcome - Welcome message
 * - msg91.template.booking.cancelled - Booking cancellation
 * 
 * SMS COST: ₹0.25 per message (Email is FREE)
 */
@Service
public class SmsService {
        @Autowired
        private CommunicationPreferenceService communicationPreferenceService;
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT_LOGGER");

    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);

    @Value("${msg91.authKey:}")
    private String authKey;

    // Sender IDs - configurable via application.properties
    @Value("${msg91.senderId.otp:FRMZOT}")
    private String SENDER_ID_OTP;
    @Value("${msg91.senderId.transactional:FMEAZY}")
    private String SENDER_ID_TRANSACTIONAL;

    // ========== TEMPLATE IDs (8 DLT-Approved Templates) ==========
    
    // FARMEAZY_LOGIN_OTP - Variables: otp, time
    @Value("${msg91.template.otp:}")
    private String otpTemplateId;

    // FARMEAZY_PASSWORD_RESET_OTP - Variables: otp, time
    @Value("${msg91.template.password.reset:}")
    private String passwordResetTemplateId;

    // FARMEAZY_PAYMENT_SUCCESS - Variables: user, rupees, orderID
    @Value("${msg91.template.payment.success:}")
    private String paymentSuccessTemplateId;

    // FARMEAZY_PAYMENT_FAILED - Variables: user, orderID
    @Value("${msg91.template.payment.failed:}")
    private String paymentFailedTemplateId;

    // FARMEAZY_WELCOME - Variables: user
    @Value("${msg91.template.welcome:}")
    private String welcomeTemplateId;

    // FARMEAZY_BOOKING_CANCELLED - Variables: user, BookingID
    @Value("${msg91.template.booking.cancelled:}")
    private String bookingCancelledTemplateId;

    // FARMEAZY_SERVICE_COMPLETED - Variables: user, serviceId
    @Value("${msg91.template.service.completed:}")
    private String serviceCompletedTemplateId;

    // FARMEAZY_IRRIGATION_REMINDER - Variables: IrrigationId, farmName
    @Value("${msg91.template.irrigation:}")
    private String irrigationTemplateId;

    // FARMEAZY_BANK_DETAILS_UPDATE_ALERT - Variables: value (added/updated/deleted)
    @Value("${msg91.template.bank.details.alert:69aaca9efd3e36f3ea0d9cf3}")
    private String bankDetailsAlertTemplateId;

    // FARMEAZY_BANK_DETAILS_ACTION_OTP - Variables: value, otp, time
    @Value("${msg91.template.bank.details.otp:69aaca53f8742775250b4d02}")
    private String bankDetailsOtpTemplateId;

    // FARMEAZY_BOOKING_CONFIRM - Variables: user, orderID
    @Value("${msg91.template.booking.confirm:69aabfdd61252f50400b5a52}")
    private String bookingConfirmTemplateId;

    // FARMEAZY_SERVICE_STARTED - Variables: user, serviceID
    @Value("${msg91.template.service.started:69aabf66097c7b8ee8088812}")
    private String serviceStartedTemplateId;

    // Legacy bank template IDs (kept for backward compatibility)
    // FARMEAZY_BANK_OTP - Variables: var (action type), otp, time
    @Value("${msg91.template.bank.otp:1107177279304266029}")
    private String bankOtpTemplateId;

    // FARMEAZY_BANK_UPDATE_ALERT - Variables: action (added/updated/deleted)
    @Value("${msg91.template.bank.alert:1107177279356200087}")
    private String bankAlertTemplateId;

    @Value("${msg91.enabled:false}")
    private boolean smsEnabled;

    private static final String MSG91_FLOW_URL = "https://api.msg91.com/api/v5/flow/";
    private static final int MAX_RETRY_ATTEMPTS = 2;

    private final RestTemplate restTemplate = new RestTemplate();

    // ========== CORE SMS METHODS (8 DLT-Approved Templates) ==========

    /**
     * Send Login OTP SMS (FARMEAZY_LOGIN_OTP)
     * Template: "Your FARMEAZY OTP is ##otp##. It is valid for ##time## minutes. Do not share."
     * Variables: otp, time
     */
    public SmsResponseDto sendOtp(String phoneNumber, String otp) {
        // Allow SMS for registration even if user preferences are missing
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        boolean isRegistration = false;
        for (StackTraceElement elem : stack) {
            if (elem.getClassName().contains("OtpService") && elem.getMethodName().contains("generateAndSendOtpWithDetails")) {
                isRegistration = true;
                break;
            }
        }
        if (!isRegistration) {
            CommunicationPreferenceResponseDto prefs = communicationPreferenceService.getPreferences(phoneNumber);
            if (prefs == null || !prefs.getOtpChannel().equals(CommunicationPreference.CommunicationChannel.SMS_ONLY) && !prefs.getOtpChannel().equals(CommunicationPreference.CommunicationChannel.BOTH)) {
                logger.info("SMS not sent due to user preference (OTP)");
                return SmsResponseDto.failure("OTP", "User preference: SMS not allowed for OTP", "SMS not allowed for OTP");
            }
        }
        return sendOtp(phoneNumber, otp, "10"); // Default 10 minutes validity
    }

    public SmsResponseDto sendOtp(String phoneNumber, String otp, String validityMinutes) {
        logger.info("SMS_LOGIN_OTP: Sending OTP to {} | message='Your FARMEAZY OTP is {}. It is valid for {} minutes. Do not share.' | params={{otp={}, time={}}}", maskPhone(phoneNumber), sanitize(otp), sanitize(validityMinutes), sanitize(otp), sanitize(validityMinutes));
        String variables = String.format("{\"otp\":\"%s\",\"time\":\"%s\"}", sanitize(otp), sanitize(validityMinutes));
        auditLogger.info("SMS REQUEST: LOGIN_OTP | phone={}, variables={}", maskPhone(phoneNumber), variables);
        SmsResponseDto response = sendFlowSms(phoneNumber, otpTemplateId, variables, "LOGIN_OTP", SENDER_ID_OTP);
        auditLogger.info("SMS RESPONSE: LOGIN_OTP | phone={}, success={}, message={}", maskPhone(phoneNumber), response.isSuccess(), response.getMessage());
        return response;
    }

    /**
     * Send Password Reset OTP SMS (FARMEAZY_PASSWORD_RESET_OTP)
     * Template: "Your FARMEAZY password reset OTP is ##otp##. It is valid for ##time## minutes."
     * Variables: otp, time
     */
    public SmsResponseDto sendPasswordResetOtp(String phoneNumber, String otp, String validityMinutes) {
        logger.info("SMS_PASSWORD_RESET: Sending password reset OTP to {} | message='Your FARMEAZY password reset OTP is {}. It is valid for {} minutes.' | params={{otp={}, time={}}}", maskPhone(phoneNumber), sanitize(otp), sanitize(validityMinutes), sanitize(otp), sanitize(validityMinutes));
        String variables = String.format("{\"otp\":\"%s\",\"time\":\"%s\"}", sanitize(otp), sanitize(validityMinutes));
        auditLogger.info("SMS REQUEST: PASSWORD_RESET_OTP | phone={}, variables={}", maskPhone(phoneNumber), variables);
        SmsResponseDto response = sendFlowSms(phoneNumber, passwordResetTemplateId, variables, "PASSWORD_RESET_OTP", SENDER_ID_OTP);
        auditLogger.info("SMS RESPONSE: PASSWORD_RESET_OTP | phone={}, success={}, message={}", maskPhone(phoneNumber), response.isSuccess(), response.getMessage());
        return response;
    }

    /**
     * Send Payment Success SMS (FARMEAZY_PAYMENT_SUCCESS)
     * Template: "Hi ##user##, payment of Rs ##rupees## for order ##orderID## was successful."
     * Variables: user, rupees, orderID
     */
    public SmsResponseDto sendPaymentSuccess(String phoneNumber, String userName, String amount, String orderId) {
        CommunicationPreferenceResponseDto prefs = communicationPreferenceService.getPreferences(phoneNumber);
        if (prefs == null || !prefs.getOrderChannel().equals(CommunicationPreference.CommunicationChannel.SMS_ONLY) && !prefs.getOrderChannel().equals(CommunicationPreference.CommunicationChannel.BOTH)) {
            logger.info("SMS not sent due to user preference (PAYMENT_SUCCESS)");
            return SmsResponseDto.failure("PAYMENT_SUCCESS", "User preference: SMS not allowed for PAYMENT_SUCCESS", "SMS not allowed for PAYMENT_SUCCESS");
        }
        logger.info("SMS_PAYMENT_SUCCESS: ₹{} for order {} to {} | message='Hi {}, payment of Rs {} for order {} was successful.' | params={{user={}, rupees={}, orderID={}}}", sanitize(amount), sanitize(orderId), maskPhone(phoneNumber), sanitize(userName), sanitize(amount), sanitize(orderId), sanitize(userName), sanitize(amount), sanitize(orderId));
        String variables = String.format("{\"user\":\"%s\",\"rupees\":\"%s\",\"orderID\":\"%s\"}", sanitize(userName), sanitize(amount), sanitize(orderId));
        auditLogger.info("SMS REQUEST: PAYMENT_SUCCESS | phone={}, variables={}", maskPhone(phoneNumber), variables);
        SmsResponseDto response = sendFlowSms(phoneNumber, paymentSuccessTemplateId, variables, "PAYMENT_SUCCESS", SENDER_ID_TRANSACTIONAL);
        auditLogger.info("SMS RESPONSE: PAYMENT_SUCCESS | phone={}, success={}, message={}", maskPhone(phoneNumber), response.isSuccess(), response.getMessage());
        return response;
    }

    /**
     * Send Payment Failed SMS (FARMEAZY_PAYMENT_FAILED)
     * Template: "Hi ##user##, your payment for order ##orderID## could not be completed."
     * Variables: user, orderID
     */
    public SmsResponseDto sendPaymentFailed(String phoneNumber, String userName, String orderId) {
        logger.info("SMS_PAYMENT_FAILED: Order {} failed for {} | message='Hi {}, your payment for order {} could not be completed.' | params={{user={}, orderID={}}}", sanitize(orderId), maskPhone(phoneNumber), sanitize(userName), sanitize(orderId), sanitize(userName), sanitize(orderId));
        String variables = String.format("{\"user\":\"%s\",\"orderID\":\"%s\"}", sanitize(userName), sanitize(orderId));
        auditLogger.info("SMS REQUEST: PAYMENT_FAILED | phone={}, variables={}", maskPhone(phoneNumber), variables);
        SmsResponseDto response = sendFlowSms(phoneNumber, paymentFailedTemplateId, variables, "PAYMENT_FAILED", SENDER_ID_TRANSACTIONAL);
        auditLogger.info("SMS RESPONSE: PAYMENT_FAILED | phone={}, success={}, message={}", maskPhone(phoneNumber), response.isSuccess(), response.getMessage());
        return response;
    }

    /**
     * Send Welcome SMS (FARMEAZY_WELCOME)
     * Template: "Welcome to FARMEAZY, ##user##! Your account has been created successfully."
     * Variables: user
     */
    public SmsResponseDto sendWelcome(String phoneNumber, String userName) {
        try {
            CommunicationPreferenceResponseDto prefs = communicationPreferenceService.getPreferences(phoneNumber);
            if (prefs == null || !prefs.getMarketingChannel().equals(CommunicationPreference.CommunicationChannel.SMS_ONLY) && !prefs.getMarketingChannel().equals(CommunicationPreference.CommunicationChannel.BOTH)) {
                logger.info("SMS not sent due to user preference (WELCOME)");
                return SmsResponseDto.failure("WELCOME", "User preference: SMS not allowed for WELCOME", "SMS not allowed for WELCOME");
            }
            logger.info("SMS_WELCOME: Sending welcome to {} | message='Welcome to FARMEAZY, {}! Your account has been created successfully.' | params={{user={}}}", maskPhone(phoneNumber), sanitize(userName), sanitize(userName));
            String variables = String.format("{\"user\":\"%s\"}", sanitize(userName));
            auditLogger.info("SMS REQUEST: WELCOME | phone={}, variables={}", maskPhone(phoneNumber), variables);
            SmsResponseDto response = sendFlowSms(phoneNumber, welcomeTemplateId, variables, "WELCOME", SENDER_ID_TRANSACTIONAL);
            auditLogger.info("SMS RESPONSE: WELCOME | phone={}, success={}, message={}", maskPhone(phoneNumber), response.isSuccess(), response.getMessage());
            return response;
        } catch (Exception e) {
            logger.warn("SMS_WELCOME: Skipping due to preference lookup/send failure for {}: {}", maskPhone(phoneNumber), e.getMessage(), e);
            return SmsResponseDto.failure("WELCOME", "Welcome SMS skipped due to communication preference lookup failure", "Welcome SMS skipped");
        }
    }

    /**
     * Send Booking Cancelled SMS (FARMEAZY_BOOKING_CANCELLED)
     * Template: "Hi ##user##, your booking ##BookingID## has been cancelled. Please contact support."
     * Variables: user, BookingID
     */
    public SmsResponseDto sendBookingCancelled(String phoneNumber, String userName, String bookingId) {
        logger.info("SMS_BOOKING_CANCELLED: Booking {} cancelled for {} | message='Hi {}, your booking {} has been cancelled. Please contact support.' | params={{user={}, BookingID={}}}", sanitize(bookingId), maskPhone(phoneNumber), sanitize(userName), sanitize(bookingId), sanitize(userName), sanitize(bookingId));
        String variables = String.format("{\"user\":\"%s\",\"BookingID\":\"%s\"}", sanitize(userName), sanitize(bookingId));
        auditLogger.info("SMS REQUEST: BOOKING_CANCELLED | phone={}, variables={}", maskPhone(phoneNumber), variables);
        SmsResponseDto response = sendFlowSms(phoneNumber, bookingCancelledTemplateId, variables, "BOOKING_CANCELLED", SENDER_ID_TRANSACTIONAL);
        auditLogger.info("SMS RESPONSE: BOOKING_CANCELLED | phone={}, success={}, message={}", maskPhone(phoneNumber), response.isSuccess(), response.getMessage());
        return response;
    }

    /**
     * Send Service Completed SMS (FARMEAZY_SERVICE_COMPLETED)
     * Template: "Hi ##user##, your service ##serviceId## has been completed successfully."
     * Variables: user, serviceId
     */
    public SmsResponseDto sendServiceCompleted(String phoneNumber, String userName, String serviceId) {
        logger.info("SMS_SERVICE_COMPLETED: Service {} completed for {} | message='Hi {}, your service {} has been completed successfully.' | params={{user={}, serviceId={}}}", sanitize(serviceId), maskPhone(phoneNumber), sanitize(userName), sanitize(serviceId), sanitize(userName), sanitize(serviceId));
        String variables = String.format("{\"user\":\"%s\",\"serviceId\":\"%s\"}", sanitize(userName), sanitize(serviceId));
        auditLogger.info("SMS REQUEST: SERVICE_COMPLETED | phone={}, variables={}", maskPhone(phoneNumber), variables);
        SmsResponseDto response = sendFlowSms(phoneNumber, serviceCompletedTemplateId, variables, "SERVICE_COMPLETED", SENDER_ID_TRANSACTIONAL);
        auditLogger.info("SMS RESPONSE: SERVICE_COMPLETED | phone={}, success={}, message={}", maskPhone(phoneNumber), response.isSuccess(), response.getMessage());
        return response;
    }

    /**
     * Send Irrigation Reminder SMS (FARMEAZY_IRRIGATION_REMINDER)
     * Template: "Reminder: Irrigation is due for ##IrrigationId## crop in your farm ##farm##."
     * Variables: IrrigationId, farm
     */
    public SmsResponseDto sendIrrigationReminder(String phoneNumber, String irrigationId, String farmName) {
        logger.info("SMS_IRRIGATION: Sending reminder for {} to {} | message='Reminder: Irrigation is due for {} crop in your farm {}.' | params={{IrrigationId={}, farm={}}}", sanitize(irrigationId), maskPhone(phoneNumber), sanitize(irrigationId), sanitize(farmName), sanitize(irrigationId), sanitize(farmName));
        String variables = String.format("{\"IrrigationId\":\"%s\",\"farm\":\"%s\"}", sanitize(irrigationId), sanitize(farmName));
        auditLogger.info("SMS REQUEST: IRRIGATION_REMINDER | phone={}, variables={}", maskPhone(phoneNumber), variables);
        SmsResponseDto response = sendFlowSms(phoneNumber, irrigationTemplateId, variables, "IRRIGATION_REMINDER", SENDER_ID_TRANSACTIONAL);
        auditLogger.info("SMS RESPONSE: IRRIGATION_REMINDER | phone={}, success={}, message={}", maskPhone(phoneNumber), response.isSuccess(), response.getMessage());
        return response;
    }

    /**
     * Send Bank Details OTP SMS (FARMEAZY_BANK_DETAILS_ACTION_OTP) - NEW DLT Template
     * Template: "Your FARMEAZY OTP for bank details ##value## verification is ##otp##. It is valid for ##time## minutes. Do not share it with anyone. - FARMEAZY"
     * Variables: value (addition/update/deletion/viewing), otp, time
     */
    public SmsResponseDto sendBankDetailsOtp(String phoneNumber, String action, String otp, String validityMinutes) {
        logger.info("SMS_BANK_DETAILS_OTP: Sending bank {} OTP to {}", action, maskPhone(phoneNumber));
        String variables = String.format("{\"value\":\"%s\",\"otp\":\"%s\",\"time\":\"%s\"}", sanitize(action), sanitize(otp), sanitize(validityMinutes));
        auditLogger.info("SMS REQUEST: BANK_DETAILS_ACTION_OTP | phone={}, variables={}", maskPhone(phoneNumber), variables);
        SmsResponseDto response = sendFlowSms(phoneNumber, bankDetailsOtpTemplateId, variables, "BANK_DETAILS_ACTION_OTP", SENDER_ID_OTP);
        auditLogger.info("SMS RESPONSE: BANK_DETAILS_ACTION_OTP | phone={}, success={}, message={}", maskPhone(phoneNumber), response.isSuccess(), response.getMessage());
        return response;
    }

    /**
     * Send Bank Details Update Alert SMS (FARMEAZY_BANK_DETAILS_UPDATE_ALERT) - NEW DLT Template
     * Template: "Your FARMEAZY bank details have been ##value## successfully. If this was not done by you, please contact support immediately. - FARMEAZY"
     * Variables: value (added/updated/deleted)
     */
    public SmsResponseDto sendBankDetailsAlert(String phoneNumber, String action) {
        logger.info("SMS_BANK_DETAILS_ALERT: Sending bank {} alert to {}", action, maskPhone(phoneNumber));
        String variables = String.format("{\"value\":\"%s\"}", sanitize(action));
        auditLogger.info("SMS REQUEST: BANK_DETAILS_UPDATE_ALERT | phone={}, variables={}", maskPhone(phoneNumber), variables);
        SmsResponseDto response = sendFlowSms(phoneNumber, bankDetailsAlertTemplateId, variables, "BANK_DETAILS_UPDATE_ALERT", SENDER_ID_TRANSACTIONAL);
        auditLogger.info("SMS RESPONSE: BANK_DETAILS_UPDATE_ALERT | phone={}, success={}, message={}", maskPhone(phoneNumber), response.isSuccess(), response.getMessage());
        return response;
    }

    /**
     * Send Booking Confirmation SMS (FARMEAZY_BOOKING_CONFIRM) - NEW DLT Template
     * Template: "Hi ##user##, your booking ##orderID## has been confirmed successfully. - FARMEAZY"
     * Variables: user, orderID
     */
    public SmsResponseDto sendBookingConfirm(String phoneNumber, String userName, String orderId) {
        logger.info("SMS_BOOKING_CONFIRM: Booking {} confirmed for {}", orderId, maskPhone(phoneNumber));
        String variables = String.format("{\"user\":\"%s\",\"orderID\":\"%s\"}", sanitize(userName), sanitize(orderId));
        auditLogger.info("SMS REQUEST: BOOKING_CONFIRM | phone={}, variables={}", maskPhone(phoneNumber), variables);
        SmsResponseDto response = sendFlowSms(phoneNumber, bookingConfirmTemplateId, variables, "BOOKING_CONFIRM", SENDER_ID_TRANSACTIONAL);
        auditLogger.info("SMS RESPONSE: BOOKING_CONFIRM | phone={}, success={}, message={}", maskPhone(phoneNumber), response.isSuccess(), response.getMessage());
        return response;
    }

    /**
     * Send Service Started SMS (FARMEAZY_SERVICE_STARTED) - NEW DLT Template
     * Template: "Hi ##user##, your service ##serviceID## has started. - FARMEAZY"
     * Variables: user, serviceID
     */
    public SmsResponseDto sendServiceStarted(String phoneNumber, String userName, String serviceId) {
        logger.info("SMS_SERVICE_STARTED: Service {} started for {}", serviceId, maskPhone(phoneNumber));
        String variables = String.format("{\"user\":\"%s\",\"serviceID\":\"%s\"}", sanitize(userName), sanitize(serviceId));
        auditLogger.info("SMS REQUEST: SERVICE_STARTED | phone={}, variables={}", maskPhone(phoneNumber), variables);
        SmsResponseDto response = sendFlowSms(phoneNumber, serviceStartedTemplateId, variables, "SERVICE_STARTED", SENDER_ID_TRANSACTIONAL);
        auditLogger.info("SMS RESPONSE: SERVICE_STARTED | phone={}, success={}, message={}", maskPhone(phoneNumber), response.isSuccess(), response.getMessage());
        return response;
    }

    /**
     * Send Bank OTP SMS (FARMEAZY_BANK_OTP) - Legacy template
     * @deprecated Use sendBankDetailsOtp() with new DLT template instead
     * Template: "Your OTP for ##var## bank details is ##otp##. Valid for ##time## minutes. Do not share."
     * Variables: var (action: addition/update/deletion/viewing), otp, time
     */
    @Deprecated
    public SmsResponseDto sendBankOtp(String phoneNumber, String action, String otp, String validityMinutes) {
        logger.info("SMS_BANK_OTP: Sending bank {} OTP to {}", action, maskPhone(phoneNumber));
        // Use new DLT template instead
        return sendBankDetailsOtp(phoneNumber, action, otp, validityMinutes);
    }

    /**
     * Send Bank Update Alert SMS (FARMEAZY_BANK_UPDATE_ALERT) - Legacy template
     * @deprecated Use sendBankDetailsAlert() with new DLT template instead
     * Template: "Your bank details have been ##action## successfully on FarmEazy."
     * Variables: action (added/updated/deleted)
     */
    @Deprecated
    public SmsResponseDto sendBankUpdateAlert(String phoneNumber, String action) {
        logger.info("SMS_BANK_ALERT: Sending bank {} alert to {}", action, maskPhone(phoneNumber));
        // Use new DLT template instead
        return sendBankDetailsAlert(phoneNumber, action);
    }

    /**
     * Legacy method - sends OTP using flow API
     * @deprecated Use sendOtp() with validity parameter instead
     */
    @Deprecated
    public void sendSms(String phoneNumber, String otp) {
        sendOtp(phoneNumber, otp);
    }

    // ========== INTERNAL METHODS ==========

    /**
     * Core method to send SMS via MSG91 Flow API
     * @param senderId FRMZOT for OTP, FMEAZY for transactional
     */
    private SmsResponseDto sendFlowSms(String phoneNumber, String templateId, String variablesJson, String smsType, String senderId) {
        SmsResponseDto response = new SmsResponseDto();
        response.setSmsType(smsType);
        response.setPhoneNumber(maskPhone(phoneNumber));

        // Check if SMS is enabled
        if (!smsEnabled) {
            logger.warn("SMS_DISABLED: SMS sending is disabled. Type={}, Phone={}", smsType, maskPhone(phoneNumber));
            response.setSuccess(false);
            response.setMessage("SMS service is disabled");
            response.setDisplayMessage("SMS notifications are currently disabled");
            return response;
        }

        // Validate phone number
        if (phoneNumber == null || phoneNumber.isBlank()) {
            logger.warn("SMS_INVALID_PHONE: Phone number is blank for type {}", smsType);
            response.setSuccess(false);
            response.setMessage("Phone number is required");
            response.setDisplayMessage("Mobile number is required for SMS");
            return response;
        }

        // Validate template ID
        if (templateId == null || templateId.isBlank()) {
            logger.error("SMS_NO_TEMPLATE: Template ID not configured for type {}", smsType);
            response.setSuccess(false);
            response.setMessage("SMS template not configured");
            response.setDisplayMessage("SMS service configuration pending");
            return response;
        }

        // Validate auth key
        if (authKey == null || authKey.isBlank()) {
            logger.error("SMS_NO_AUTH: MSG91 auth key not configured");
            response.setSuccess(false);
            response.setMessage("SMS auth key not configured");
            response.setDisplayMessage("SMS service configuration pending");
            return response;
        }

        // Format phone number (ensures 91 prefix)
        String formattedPhone = formatPhoneNumber(phoneNumber);
        logger.info("SMS_PHONE: Formatted to {} (91XXXXXXXXXX format)", maskPhone(formattedPhone));

        // Prepare request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authkey", authKey);

        // Build request body - merge flow_id, sender, mobiles with variables
        String requestBody = buildRequestBody(templateId, formattedPhone, variablesJson, senderId);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // Send with retry
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                ResponseEntity<String> apiResponse = restTemplate.postForEntity(MSG91_FLOW_URL, entity, String.class);
                
                if (apiResponse.getStatusCode().is2xxSuccessful()) {
                    logger.info("SMS_SENT: Type={}, Phone={}, Response={}", smsType, maskPhone(phoneNumber), apiResponse.getBody());
                    response.setSuccess(true);
                    response.setMessage("SMS sent successfully");
                    response.setDisplayMessage(getSuccessMessage(smsType));
                    response.setApiResponse(apiResponse.getBody());
                    return response;
                } else {
                    lastException = new RuntimeException("API returned: " + apiResponse.getStatusCode());
                }
            } catch (Exception e) {
                lastException = e;
                logger.warn("SMS_ATTEMPT_FAILED: Type={}, Attempt={}, Error={}", smsType, attempts + 1, e.getMessage());
            }
            attempts++;
        }

        // All retries failed
        logger.error("SMS_FAILED: Type={}, Phone={}, Error={}", smsType, maskPhone(phoneNumber), 
            lastException != null ? lastException.getMessage() : "Unknown error");
        response.setSuccess(false);
        response.setMessage("Failed to send SMS after " + MAX_RETRY_ATTEMPTS + " attempts");
        response.setDisplayMessage("SMS could not be sent. Please check your mobile number.");
        return response;
    }

    /**
     * Build MSG91 Flow API request body
     * @param senderId FRMZOT for OTP, FMEAZY for transactional
     */
    private String buildRequestBody(String templateId, String phone, String variablesJson, String senderId) {
        // Parse variables JSON and merge with flow parameters
        StringBuilder body = new StringBuilder();
        body.append("{");
        body.append("\"flow_id\":\"").append(templateId).append("\",");
        body.append("\"sender\":\"").append(senderId).append("\",");
        body.append("\"mobiles\":\"").append(phone).append("\"");
        
        // Add variables if present
        if (variablesJson != null && !variablesJson.equals("{}")) {
            // Remove outer braces and append
            String vars = variablesJson.trim();
            if (vars.startsWith("{")) vars = vars.substring(1);
            if (vars.endsWith("}")) vars = vars.substring(0, vars.length() - 1);
            if (!vars.isBlank()) {
                body.append(",").append(vars);
            }
        }
        
        body.append("}");
        return body.toString();
    }

    /**
     * Format phone number to MSG91 format (91XXXXXXXXXX)
     * Always ensures 91 country code prefix for India
     */
    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return phoneNumber;
        }
        
        // Remove all non-digit characters
        String digits = phoneNumber.replaceAll("[^0-9]", "");
        
        // Remove leading zero if present
        if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        
        // Handle various formats:
        // 1. 10 digits (9876543210) -> add 91
        // 2. 12 digits starting with 91 (919876543210) -> keep as is
        // 3. 11 digits starting with 91 but missing one digit -> invalid
        // 4. 13+ digits with extra 91 prefix -> normalize
        
        if (digits.length() == 10) {
            // Standard 10-digit Indian number - add 91 prefix
            digits = "91" + digits;
        } else if (digits.length() == 12 && digits.startsWith("91")) {
            // Already has 91 prefix - keep as is
        } else if (digits.length() > 12 && digits.startsWith("91")) {
            // Has 91 prefix but extra digits - take last 10 and add 91
            digits = "91" + digits.substring(digits.length() - 10);
        } else if (digits.length() == 11 && digits.startsWith("91")) {
            // Missing one digit after 91 - invalid, but try to use as is
            logger.warn("SMS_PHONE_FORMAT: Unusual phone format: {}", digits);
        }
        
        logger.debug("SMS_PHONE_FORMAT: {} -> {}", maskPhone(phoneNumber), maskPhone(digits));
        return digits;
    }

    /**
     * Mask phone number for logging (privacy)
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return phone.substring(0, 2) + "****" + phone.substring(phone.length() - 2);
    }

    /**
     * Sanitize string for JSON
     */
    private String sanitize(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }

    /**
     * Get user-friendly success message based on SMS type
     */
    private String getSuccessMessage(String smsType) {
        return switch (smsType) {
            case "OTP" -> "OTP sent to your mobile number";
            case "BOOKING_CONFIRMATION" -> "Booking confirmation SMS sent";
            case "SERVICE_STARTED" -> "Service started notification sent";
            case "SERVICE_COMPLETED" -> "Service completion notification sent";
            case "IRRIGATION_REMINDER" -> "Irrigation reminder sent";
            case "PAYMENT_SUCCESS" -> "Payment confirmation SMS sent";
            case "PAYMENT_FAILED" -> "Payment status SMS sent";
            case "BANK_OTP" -> "Bank verification OTP sent";
            case "BANK_UPDATE_ALERT" -> "Bank details update notification sent";
            default -> "SMS sent successfully";
        };
    }

    // ========== UTILITY METHODS ==========

    /**
     * Check if SMS service is configured and enabled
     */
    public boolean isConfigured() {
        return smsEnabled && authKey != null && !authKey.isBlank() && !authKey.equals("your-local-msg91-auth-key");
    }

    /**
     * Get SMS service status for health check
     */
    public String getStatus() {
        if (!smsEnabled) return "DISABLED";
        if (authKey == null || authKey.isBlank()) return "NOT_CONFIGURED";
        if (authKey.equals("your-local-msg91-auth-key")) return "USING_PLACEHOLDER";
        return "READY";
    }

    /**
     * Send a specific SMS template with dummy or provided variables.
     * This bypasses communication preference checks for template validation.
     */
    public SmsResponseDto sendTemplateTest(String phoneNumber, SmsTemplate template, Map<String, String> inputValues) {
        if (template == null) {
            return SmsResponseDto.failure("TEMPLATE_TEST", "Template is required", "Template is required");
        }

        Map<String, String> normalized = new LinkedHashMap<>();
        for (String variable : template.getVariables()) {
            String value = inputValues == null ? null : inputValues.get(variable);
            if (value == null || value.isBlank()) {
                value = defaultTemplateValue(variable);
            }
            normalized.put(variable, sanitize(value));
        }

        StringBuilder variablesJson = new StringBuilder("{");
        int index = 0;
        for (Map.Entry<String, String> entry : normalized.entrySet()) {
            if (index++ > 0) {
                variablesJson.append(",");
            }
            variablesJson.append("\"")
                    .append(entry.getKey())
                    .append("\":\"")
                    .append(entry.getValue())
                    .append("\"");
        }
        variablesJson.append("}");

        return sendFlowSms(
                phoneNumber,
                template.getTemplateId(),
                variablesJson.toString(),
                "TEMPLATE_TEST_" + template.name(),
                template.getSenderId()
        );
    }

    private String defaultTemplateValue(String variable) {
        String normalized = variable == null ? "" : variable.trim().toLowerCase();
        return switch (normalized) {
            case "otp" -> "123456";
            case "time" -> "10";
            case "user" -> "FarmEazy User";
            case "rupees" -> "199";
            case "orderid", "bookingid" -> "FZ1001";
            case "serviceid" -> "SV1001";
            case "irrigationid" -> "IR1001";
            case "farm", "farmname" -> "Demo Farm";
            case "value" -> "updated";
            default -> "demo";
        };
    }
}
