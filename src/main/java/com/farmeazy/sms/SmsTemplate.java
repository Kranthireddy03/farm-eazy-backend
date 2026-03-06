package com.farmeazy.sms;

/**
 * SMS Template enum for FarmEazy MSG91 DLT-approved templates.
 * 
 * Each template has:
 * - Template ID (DLT approved)
 * - Sender ID (FRMZOT for OTP, FMEAZY for transactional)
 * - Variable names required
 */
public enum SmsTemplate {
    
    // OTP Templates (Sender: FRMZOT)
    LOGIN_OTP(
        "69aa80d4663b879f870c0c83",
        "FRMZOT",
        "Your FARMEAZY OTP is ##otp##. It is valid for ##time## minutes. Do not share it with anyone. - FARMEAZY",
        new String[]{"otp", "time"}
    ),
    
    PASSWORD_RESET_OTP(
        "69aa83bec68f82ba17084ca2",
        "FRMZOT",
        "Your FARMEAZY password reset OTP is ##otp##. It is valid for ##time## minutes. Do not share it with anyone. - FARMEAZY",
        new String[]{"otp", "time"}
    ),
    
    // Transactional Templates (Sender: FMEAZY)
    WELCOME(
        "69aa82992b688ae615049392",
        "FMEAZY",
        "Welcome to FARMEAZY, ##user##! Your account has been created successfully. Happy farming.",
        new String[]{"user"}
    ),
    
    PAYMENT_SUCCESS(
        "69aa830407d69373d20a97d3",
        "FMEAZY",
        "Hi ##user##, payment of Rs ##rupees## for order ##orderID## was successful. Thank you for using FARMEAZY.",
        new String[]{"user", "rupees", "orderID"}
    ),
    
    PAYMENT_FAILED(
        "69aa8419579b232182019242",
        "FMEAZY",
        "Hi ##user##, your payment for order ##orderID## could not be completed. Please try again. - FARMEAZY.",
        new String[]{"user", "orderID"}
    ),
    
    BOOKING_CANCELLED(
        "69aa835909a2e8fb040ad7f3",
        "FMEAZY",
        "Hi ##user##, your booking ##BookingID## has been cancelled. Please contact support if you need assistance. - FARMEAZY",
        new String[]{"user", "BookingID"}
    ),
    
    SERVICE_COMPLETED(
        "69aa817b72381a761f0e5052",
        "FMEAZY",
        "Hi ##user##, your service ##serviceId## has been completed successfully. Thank you for using FARMEAZY.",
        new String[]{"user", "serviceId"}
    ),
    
    IRRIGATION_REMINDER(
        "69aa82046a9b3765ba0ad9a3",
        "FMEAZY",
        "Reminder: Irrigation is due for ##IrrigationId## crop in your farm ##farm##. - FARMEAZY",
        new String[]{"IrrigationId", "farm"}
    ),
    
    // Bank Details Templates
    BANK_DETAILS_UPDATE_ALERT(
        "69aaca9efd3e36f3ea0d9cf3",
        "FMEAZY",
        "Your FARMEAZY bank details have been ##value## successfully. If this was not done by you, please contact support immediately. - FARMEAZY",
        new String[]{"value"}
    ),
    
    BANK_DETAILS_ACTION_OTP(
        "69aaca53f8742775250b4d02",
        "FRMZOT",
        "Your FARMEAZY OTP for bank details ##value## verification is ##otp##. It is valid for ##time## minutes. Do not share it with anyone. - FARMEAZY",
        new String[]{"value", "otp", "time"}
    ),
    
    // Booking/Order Templates
    BOOKING_CONFIRM(
        "69aabfdd61252f50400b5a52",
        "FMEAZY",
        "Hi ##user##, your booking ##orderID## has been confirmed successfully. - FARMEAZY",
        new String[]{"user", "orderID"}
    ),
    
    SERVICE_STARTED(
        "69aabf66097c7b8ee8088812",
        "FMEAZY",
        "Hi ##user##, your service ##serviceID## has started. - FARMEAZY",
        new String[]{"user", "serviceID"}
    );
    
    private final String templateId;
    private final String senderId;
    private final String content;
    private final String[] variables;
    
    SmsTemplate(String templateId, String senderId, String content, String[] variables) {
        this.templateId = templateId;
        this.senderId = senderId;
        this.content = content;
        this.variables = variables;
    }
    
    public String getTemplateId() {
        return templateId;
    }
    
    public String getSenderId() {
        return senderId;
    }
    
    public String getContent() {
        return content;
    }
    
    public String[] getVariables() {
        return variables;
    }
    
    /**
     * Check if this is an OTP template (uses FRMZOT sender)
     */
    public boolean isOtpTemplate() {
        return "FRMZOT".equals(senderId);
    }
    
    /**
     * Check if this is a transactional template (uses FMEAZY sender)
     */
    public boolean isTransactionalTemplate() {
        return "FMEAZY".equals(senderId);
    }
}
