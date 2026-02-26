package com.farmeazy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * EMAIL DTO - EMAIL REQUEST DATA TRANSFER OBJECT
 * 
 * PURPOSE: Transfer email data for sending messages to users.
 * Used for generic email sending operations.
 * 
 * @author FarmEazy Development Team
 * @version 1.0.0
 * @since January 2026
 */
public class EmailDto {
    
    /**
     * Recipient email address.
     * Must be a valid email format.
     */
    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String to;
    
    /**
     * Email subject line.
     */
    @NotBlank(message = "Email subject is required")
    private String subject;
    
    /**
     * Email body content (plain text or HTML).
     */
    @NotBlank(message = "Email body is required")
    private String body;
    
    /**
     * Flag to indicate if body contains HTML content.
     * Default: false (plain text)
     */
    private boolean html = false;
    
    // Constructors
    public EmailDto() {}
    
    public EmailDto(String to, String subject, String body, boolean html) {
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.html = html;
    }
    
    // Getters and Setters
    public String getTo() {
        return to;
    }
    
    public void setTo(String to) {
        this.to = to;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getBody() {
        return body;
    }
    
    public void setBody(String body) {
        this.body = body;
    }
    
    public boolean isHtml() {
        return html;
    }
    
    public void setHtml(boolean html) {
        this.html = html;
    }
}
