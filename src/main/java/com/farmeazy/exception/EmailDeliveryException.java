package com.farmeazy.exception;

/**
 * Exception thrown when email delivery fails.
 * 
 * This exception is used to indicate that the email service
 * was unable to send an email due to various reasons:
 * - Domain not verified
 * - API key invalid
 * - Service unavailable
 * - Rate limiting
 * - Other delivery failures
 * 
 * @author FarmEazy Development Team
 */
public class EmailDeliveryException extends RuntimeException {
    
    private final String errorCode;
    private final String recipientEmail;

    public EmailDeliveryException(String message) {
        super(message);
        this.errorCode = "EMAIL_DELIVERY_FAILED";
        this.recipientEmail = null;
    }

    public EmailDeliveryException(String message, String recipientEmail) {
        super(message);
        this.errorCode = "EMAIL_DELIVERY_FAILED";
        this.recipientEmail = recipientEmail;
    }

    public EmailDeliveryException(String message, String errorCode, String recipientEmail) {
        super(message);
        this.errorCode = errorCode;
        this.recipientEmail = recipientEmail;
    }

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "EMAIL_DELIVERY_FAILED";
        this.recipientEmail = null;
    }

    public EmailDeliveryException(String message, String recipientEmail, Throwable cause) {
        super(message, cause);
        this.errorCode = "EMAIL_DELIVERY_FAILED";
        this.recipientEmail = recipientEmail;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }
}
