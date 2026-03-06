package com.farmeazy.service;

/**
 * EMAIL TYPE ENUMERATION
 * 
 * PURPOSE: Categorizes emails to determine which sender address to use.
 * This ensures emails are sent from the appropriate account based on their purpose.
 * 
 * SENDER MAPPING:
 * - NOREPLY: no-reply@farm-eazy.com - Automated notifications, users should NOT reply
 * - INFO: info@farm-eazy.com - Informational emails, general updates
 * - SUPPORT: support@farm-eazy.com - Support-related communications
 * 
 * USAGE GUIDELINES:
 * - NOREPLY: OTP codes, verification emails, system alerts, order confirmations
 * - INFO: Welcome emails, product listings, newsletters, promotional content
 * - SUPPORT: Password reset, bank issues, account problems, help requests
 * 
 * @author FarmEazy Development Team
 * @version 1.0.0
 * @since March 2026
 */
public enum EmailType {
    /**
     * No-Reply emails - Automated system notifications
     * Users should NOT reply to these emails
     * Examples: OTP, verification codes, order confirmations, payment receipts
     */
    NOREPLY,
    
    /**
     * Info emails - Informational and promotional content
     * Users can reply for general inquiries
     * Examples: Welcome emails, product listings, newsletters, updates
     */
    INFO,
    
    /**
     * Support emails - Support and help-related communications
     * Users should reply if they need assistance
     * Examples: Password reset, bank verification issues, account problems
     */
    SUPPORT
}
