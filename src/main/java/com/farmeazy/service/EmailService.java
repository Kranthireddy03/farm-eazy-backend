package com.farmeazy.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * EMAIL SERVICE - EMAIL SENDING FUNCTIONALITY
 * 
 * PURPOSE: Centralized email service for sending notifications to users.
 * Supports both plain text and HTML emails.
 * 
 * FEATURES:
 * 1. Welcome emails on user registration
 * 2. Password reset emails
 * 3. Irrigation schedule reminders
 * 4. Crop harvest notifications
 * 5. Generic email sending
 * 
 * CONFIGURATION:
 * - spring.mail.host: SMTP server host
 * - spring.mail.port: SMTP server port (587 for TLS)
 * - spring.mail.username: Email account username
 * - spring.mail.password: Email account password (use App Password for Gmail)
 * - farmeazy.mail.from: Default sender email address
 * - farmeazy.mail.enabled: Enable/disable email sending
 * 
 * GMAIL SETUP:
 * 1. Enable 2-factor authentication on your Google account
 * 2. Generate App Password at: https://myaccount.google.com/apppasswords
 * 3. Use the generated 16-character password in application.properties
 * 
 * USAGE EXAMPLE:
 * emailService.sendWelcomeEmail("user@example.com", "John Doe");
 * emailService.sendPasswordResetEmail("user@example.com", "reset-token-123");
 * 
 * @author FarmEazy Development Team
 * @version 1.0.0
 * @since January 2026
 */
@Service
public class EmailService {
    /**
     * Sends password changed confirmation email.
     * Uses SUPPORT sender as this is a security-related notification.
     * @param to Recipient email address
     * @param fullName User's full name
     */
    public void sendPasswordChangedConfirmation(String to, String fullName) {
        String subject = "Your FarmEazy password was changed";
        String body = "Hello " + fullName + ",\n\nYour password was changed successfully. If you did not perform this action, please contact support immediately at support@farm-eazy.com.\n\nRegards,\nFarmEazy Support";
        sendEmail(to, subject, body, EmailType.SUPPORT);
    }

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    @org.springframework.beans.factory.annotation.Qualifier("noReplyMailSender")
    private JavaMailSender noReplyMailSender;

    @Autowired(required = false)
    @org.springframework.beans.factory.annotation.Qualifier("supportMailSender")
    private JavaMailSender supportMailSender;

    @Autowired(required = false)
    @org.springframework.beans.factory.annotation.Qualifier("infoMailSender")
    private JavaMailSender infoMailSender;

    @Autowired(required = false)
    private UnifiedEmailService unifiedEmailService;

    @Value("${farmeazy.mail.from}")
    private String fromEmail;

    @Value("${farmeazy.mail.enabled:false}")
    private boolean emailEnabled;

    @Value("${farmeazy.app.base-url:https://farm-eazy-backend.onrender.com}")
    private String appBaseUrl;

    // Multi-sender email addresses
    @Value("${farmeazy.mail.noreply:no-reply@farm-eazy.com}")
    private String noReplyEmail;

    @Value("${farmeazy.mail.info:info@farm-eazy.com}")
    private String infoEmail;

    @Value("${farmeazy.mail.support:support@farm-eazy.com}")
    private String supportEmail;

    /**
     * Gets the appropriate sender email address based on email type.
     * 
     * @param emailType The type of email being sent
     * @return The sender email address for this type
     */
    private String getSenderEmail(EmailType emailType) {
        return switch (emailType) {
            case NOREPLY -> noReplyEmail;
            case INFO -> infoEmail;
            case SUPPORT -> supportEmail;
        };
    }

    private String getAppBaseUrl() {
        return System.getenv().getOrDefault("APP_BASE_URL", appBaseUrl);
    }

    private UnifiedEmailService.SenderType mapSenderType(EmailType emailType) {
        return switch (emailType) {
            case NOREPLY -> UnifiedEmailService.SenderType.NOREPLY;
            case INFO -> UnifiedEmailService.SenderType.INFO;
            case SUPPORT -> UnifiedEmailService.SenderType.SUPPORT;
        };
    }

    private String toHtml(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br/>");
    }

    /**
     * Sends a plain text email using the default sender.
     * 
     * @param to Recipient email address
     * @param subject Email subject
     * @param body Email body (plain text)
     */
    public void sendEmail(String to, String subject, String body) {
        sendEmail(to, subject, body, EmailType.NOREPLY);
    }

    /**
     * Sends a plain text email using the specified sender type.
     * 
     * @param to Recipient email address
     * @param subject Email subject
     * @param body Email body (plain text)
     * @param emailType The type of email (determines sender address)
     */
    public void sendEmail(String to, String subject, String body, EmailType emailType) {
        if (!emailEnabled) {
            logger.info("Email sending is disabled. Would have sent to: {}", to);
            throw new RuntimeException("Email sending is disabled.");
        }

        String senderEmail = getSenderEmail(emailType);
        JavaMailSender selectedSender = null;
        if (senderEmail.equalsIgnoreCase(noReplyEmail)) {
            selectedSender = noReplyMailSender;
        } else if (senderEmail.equalsIgnoreCase(supportEmail)) {
            selectedSender = supportMailSender;
        } else if (senderEmail.equalsIgnoreCase(infoEmail)) {
            selectedSender = infoMailSender;
        } else {
            selectedSender = noReplyMailSender;
        }
        int attempts = 0;
        boolean sent = false;
        Exception lastException = null;
        while (!sent && attempts < 2) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(senderEmail);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                if (selectedSender != null) {
                    selectedSender.send(message);
                    logger.info("Email sent successfully to: {} from: {}", to, senderEmail);
                    sent = true;
                } else {
                    logger.error("No JavaMailSender bean found for sender {}", senderEmail);
                    throw new RuntimeException("No JavaMailSender bean found for sender " + senderEmail);
                }
            } catch (Exception e) {
                attempts++;
                lastException = e;
                logger.error("Failed to send email to: {} from: {} (attempt {})", to, senderEmail, attempts, e);
            }
        }
        if (!sent) {
            if (unifiedEmailService != null) {
                boolean fallbackSent = unifiedEmailService.sendEmail(
                        to,
                        subject,
                        toHtml(body),
                        mapSenderType(emailType)
                );
                if (fallbackSent) {
                    logger.info("Email fallback succeeded via UnifiedEmailService for {}", to);
                    return;
                }
            }
            throw new RuntimeException("Communication failed: Email could not be sent. Please retry.", lastException);
        }
    }

    /**
     * Sends an HTML email using the default sender.
     * 
     * @param to Recipient email address
     * @param subject Email subject
     * @param htmlBody Email body (HTML content)
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        sendHtmlEmail(to, subject, htmlBody, EmailType.NOREPLY);
    }

    /**
     * Sends an HTML email using the specified sender type.
     * 
     * @param to Recipient email address
     * @param subject Email subject
     * @param htmlBody Email body (HTML content)
     * @param emailType The type of email (determines sender address)
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody, EmailType emailType) {
        if (!emailEnabled) {
            logger.info("Email sending is disabled. Would have sent HTML email to: {}", to);
            return;
        }

        String senderEmail = getSenderEmail(emailType);
        JavaMailSender selectedSender = null;
        if (senderEmail.equalsIgnoreCase(noReplyEmail)) {
            selectedSender = noReplyMailSender;
        } else if (senderEmail.equalsIgnoreCase(supportEmail)) {
            selectedSender = supportMailSender;
        } else if (senderEmail.equalsIgnoreCase(infoEmail)) {
            selectedSender = infoMailSender;
        } else {
            selectedSender = noReplyMailSender;
        }
        try {
            MimeMessage message = selectedSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = isHtml
            selectedSender.send(message);
            logger.info("HTML email sent successfully to: {} from: {}", to, senderEmail);
        } catch (Exception e) {
            logger.error("Failed to send HTML email to: {} from: {}", to, senderEmail, e);
            if (unifiedEmailService != null) {
                boolean fallbackSent = unifiedEmailService.sendEmail(
                        to,
                        subject,
                        htmlBody,
                        mapSenderType(emailType)
                );
                if (fallbackSent) {
                    logger.info("HTML email fallback succeeded via UnifiedEmailService for {}", to);
                    return;
                }
            }
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    /**
     * Sends welcome email to newly registered user.
     * 
     * @param userEmail User's email address
     * @param userName User's full name
     */
    public void sendWelcomeEmail(String userEmail, String userName) {
        sendWelcomeEmailHtml(userEmail, userName);
    }

    /**
     * Sends password reset email with reset token.
     * 
     * @param userEmail User's email address
     * @param shortCode Short 8-character code for password reset
     */
    public void sendPasswordResetEmail(String userEmail, String userName, String shortCode) {
        sendPasswordResetEmailHtml(userEmail, userName, shortCode);
    }

    /**
     * Sends irrigation reminder email.
     * 
     * @param userEmail User's email address
     * @param farmName Farm name
     * @param cropName Crop name
     * @param scheduledTime Scheduled irrigation time
     */
    public void sendIrrigationReminder(String userEmail, String userName, String farmName, String cropName, String scheduledTime) {
        sendIrrigationReminderHtml(userEmail, userName, farmName, cropName, scheduledTime);
    }

    /**
     * Sends a professional HTML irrigation reminder email.
     *
     * @param userEmail User's email address
     * @param userName User's name
     * @param farmName Farm name
     * @param cropName Crop name
     * @param scheduledTime Scheduled irrigation time
     */
    private void sendIrrigationReminderHtml(String userEmail, String userName, String farmName, String cropName, String scheduledTime) {
        String subject = "⏰ FarmEazy Irrigation Reminder: " + cropName + " at " + farmName;
        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 20px auto; background-color: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #3b82f6 0%%, #2563eb 100%%); color: white; padding: 30px; text-align: center; }
                    .content { padding: 30px; }
                    .greeting { font-size: 18px; }
                    .details-table { width: 100%%; border-collapse: collapse; margin: 20px 0; }
                    .details-table th, .details-table td { padding: 12px; text-align: left; border-bottom: 1px solid #e5e7eb; }
                    .details-table th { background-color: #f9fafb; font-weight: 600; }
                    .footer { background-color: #f9fafb; padding: 20px; text-align: center; font-size: 12px; color: #666; border-top: 1px solid #e5e7eb; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Irrigation Reminder</h1>
                    </div>
                    <div class="content">
                        <p class="greeting">Hello <strong>%s</strong>,</p>
                        <p>This is a friendly reminder for your upcoming irrigation schedule. Timely watering is crucial for a healthy harvest.</p>
                        <table class="details-table">
                            <tr>
                                <th>Farm</th>
                                <td>%s</td>
                            </tr>
                            <tr>
                                <th>Crop</th>
                                <td>%s</td>
                            </tr>
                            <tr>
                                <th>Scheduled Time</th>
                                <td>%s</td>
                            </tr>
                        </table>
                        <p>Please ensure your irrigation system is ready.</p>
                    </div>
                    <div class="footer">
                        <p>You can manage your schedules in the FarmEazy dashboard.</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, farmName, cropName, scheduledTime);

        sendHtmlEmail(userEmail, subject, htmlBody, EmailType.INFO);
        logger.info("HTML Irrigation Reminder email sent to: {} for farm: {} (from: info@farm-eazy.com)", userEmail, farmName);
    }

    /**
     * Sends a professional HTML harvest notification email.
     *
     * @param userEmail User's email address
     * @param userName User's name
     * @param farmName Farm name
     * @param cropName Crop name
     * @param estimatedDate Estimated harvest date
     */
    private void sendHarvestNotificationHtml(String userEmail, String userName, String farmName, String cropName, String estimatedDate) {
        String subject = "🌾 Harvest Time Approaching for " + cropName;
        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 20px auto; background-color: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #f59e0b 0%%, #d97706 100%%); color: white; padding: 30px; text-align: center; }
                    .content { padding: 30px; }
                    .greeting { font-size: 18px; }
                    .details-table { width: 100%%; border-collapse: collapse; margin: 20px 0; }
                    .details-table th, .details-table td { padding: 12px; text-align: left; border-bottom: 1px solid #e5e7eb; }
                    .details-table th { background-color: #f9fafb; font-weight: 600; }
                    .cta-button { display: inline-block; background: linear-gradient(135deg, #f59e0b 0%%, #d97706 100%%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 4px; margin-top: 20px; }
                    .footer { background-color: #f9fafb; padding: 20px; text-align: center; font-size: 12px; color: #666; border-top: 1px solid #e5e7eb; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Harvest Notification</h1>
                    </div>
                    <div class="content">
                        <p class="greeting">Hello <strong>%s</strong>,</p>
                        <p>Get ready! The harvest time for your crop is just around the corner. Here are the details:</p>
                        <table class="details-table">
                            <tr>
                                <th>Farm</th>
                                <td>%s</td>
                            </tr>
                            <tr>
                                <th>Crop</th>
                                <td>%s</td>
                            </tr>
                            <tr>
                                <th>Estimated Harvest Date</th>
                                <td>%s</td>
                            </tr>
                        </table>
                        <p>We recommend preparing your harvesting equipment and labor. Once harvested, you can easily list your produce on the FarmEazy marketplace.</p>
                        <a href="%s/products/new" class="cta-button">List Your Produce Now</a>
                    </div>
                    <div class="footer">
                        <p>Wishing you a bountiful harvest!</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, farmName, cropName, estimatedDate, getAppBaseUrl());

        sendHtmlEmail(userEmail, subject, htmlBody, EmailType.INFO);
        logger.info("HTML Harvest Notification email sent to: {} for crop: {} (from: info@farm-eazy.com)", userEmail, cropName);
    }

    /**
     * Sends crop harvest notification email.
     * 
     * @param userEmail User's email address
     * @param farmName Farm name
     * @param cropName Crop name
     * @param estimatedDate Estimated harvest date
     */
    public void sendHarvestNotification(String userEmail, String userName, String farmName, String cropName, String estimatedDate) {
        sendHarvestNotificationHtml(userEmail, userName, farmName, cropName, estimatedDate);
    }

    /**
     * Sends custom notification email.
     * 
     * @param userEmail User's email address
     * @param subject Email subject
     * @param message Email message
     */
    public void sendNotification(String userEmail, String userName, String subject, String message) {
        sendNotificationHtml(userEmail, userName, subject, message);
    }

    /**
     * Sends notification email (alias for sendNotification).
     * Uses INFO sender by default for general notifications.
     * 
     * @param userEmail User's email address
     * @param userName User's name
     * @param subject Email subject
     * @param message Email message
     */
    public void sendNotificationEmail(String userEmail, String userName, String subject, String message) {
        sendNotificationHtml(userEmail, userName, subject, message, EmailType.INFO);
    }

    /**
     * Sends notification email with specific sender type.
     * Use SUPPORT for security-related notifications (bank details, account changes).
     * 
     * @param userEmail User's email address
     * @param userName User's name
     * @param subject Email subject
     * @param message Email message
     * @param emailType The type of email (determines sender address)
     */
    public void sendNotificationEmail(String userEmail, String userName, String subject, String message, EmailType emailType) {
        sendNotificationHtml(userEmail, userName, subject, message, emailType);
    }

    /**
     * Sends OTP (One-Time Password) verification email.
     * 
     * @param userEmail User's email address
     * @param otpCode 6-digit OTP code
     * @param purpose Purpose of OTP (SELLING, BUYING, etc.)
     */
    public void sendOtpEmail(String userEmail, String userName, String otpCode, String purpose) {
        sendOtpEmailHtml(userEmail, userName, otpCode, purpose);
    }

    /**
     * Sends a professional HTML OTP email.
     *
     * @param userEmail User's email address
     * @param userName User's name
     * @param otpCode 6-digit OTP code
     * @param purpose Purpose of the OTP
     */
    public void sendOtpEmailHtml(String userEmail, String userName, String otpCode, String purpose) {
        String subject = "🔐 Your FarmEazy Verification Code";
        String purposeText = purpose.equals("SELLING") ? "your product listing" : 
                           purpose.equals("BUYING") ? "your purchase" : "your verification";

        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 20px auto; background-color: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #f59e0b 0%%, #d97706 100%%); color: white; padding: 30px; text-align: center; }
                    .content { padding: 30px; }
                    .otp-code { font-size: 36px; font-weight: bold; text-align: center; letter-spacing: 8px; margin: 20px 0; padding: 15px; background-color: #f3f4f6; border-radius: 4px; }
                    .warning { background-color: #fefce8; border-left: 4px solid #f59e0b; padding: 15px; margin-top: 20px; border-radius: 4px; }
                    .footer { background-color: #f9fafb; padding: 20px; text-align: center; font-size: 12px; color: #666; border-top: 1px solid #e5e7eb; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>One-Time Password</h1>
                    </div>
                    <div class="content">
                        <p>Hello <strong>%s</strong>,</p>
                        <p>Please use the following verification code to complete %s. This code is valid for 10 minutes.</p>
                        
                        <div class="otp-code">%s</div>
                        
                        <div class="warning">
                            <strong>Security First:</strong> For your protection, do not share this code with anyone. FarmEazy will never ask for your OTP over the phone or in an email.
                        </div>
                    </div>
                    <div class="footer">
                        <p>If you did not request this code, please ignore this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, purposeText, otpCode);

        sendHtmlEmail(userEmail, subject, htmlBody, EmailType.NOREPLY);
        logger.info("HTML OTP email sent to: {} for purpose: {} (from: no-reply@farm-eazy.com)", userEmail, purpose);
    }

    /**
     * Sends product listing confirmation email.
     * 
     * @param userEmail User's email address
     * @param productName Name of the listed product
     * @param category Product category
     * @param price Product price
     * @param quantity Product quantity
     */
    public void sendProductListingConfirmation(String userEmail, String userName, String productName, String category, Double price, Integer quantity, String unit) {
        sendProductListingConfirmationHtml(userEmail, userName, productName, category, price, quantity, unit);
        logger.info("Product listing confirmation email sent to: {} for product: {}", userEmail, productName);
    }

    /**
     * Sends professional HTML welcome email to newly registered user.
     * 
     * @param userEmail User's email address
     * @param userName User's full name or username
     */
    public void sendWelcomeEmailHtml(String userEmail, String userName) {
        String subject = "Welcome to FarmEazy! 🌾 Start Your Smart Farming Journey";
        
        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        background-color: #f5f5f5;
                    }
                    .container {
                        max-width: 600px;
                        margin: 20px auto;
                        background-color: white;
                        border-radius: 8px;
                        overflow: hidden;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    .header {
                        background: linear-gradient(135deg, #10b981 0%%, #059669 100%%);
                        color: white;
                        padding: 30px;
                        text-align: center;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 28px;
                    }
                    .content {
                        padding: 30px;
                    }
                    .greeting {
                        font-size: 18px;
                        margin-bottom: 20px;
                    }
                    .features {
                        background-color: #f0fdf4;
                        border-left: 4px solid #10b981;
                        padding: 15px;
                        margin: 20px 0;
                        border-radius: 4px;
                    }
                    .feature-item {
                        display: flex;
                        align-items: center;
                        margin: 10px 0;
                    }
                    .feature-icon {
                        font-size: 20px;
                        margin-right: 12px;
                        width: 30px;
                    }
                    .cta-button {
                        display: inline-block;
                        background: linear-gradient(135deg, #10b981 0%%, #059669 100%%);
                        color: white;
                        padding: 12px 30px;
                        text-decoration: none;
                        border-radius: 4px;
                        margin: 20px 0;
                        font-weight: 600;
                    }
                    .footer {
                        background-color: #f9fafb;
                        padding: 20px;
                        text-align: center;
                        font-size: 12px;
                        color: #666;
                        border-top: 1px solid #e5e7eb;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🌾 Welcome to FarmEazy</h1>
                        <p>Your Smart Farm Management System</p>
                    </div>
                    
                    <div class="content">
                        <div class="greeting">
                            <p>Hello <strong>%s</strong>,</p>
                            <p>Welcome aboard! We're thrilled to have you join the FarmEazy family. Let's make your farming journey smarter and more productive.</p>
                        </div>
                        
                        <h3 style="color: #10b981;">What You Can Do with FarmEazy:</h3>
                        <div class="features">
                            <div class="feature-item">
                                <span class="feature-icon">🌾</span>
                                <span>Manage multiple farms and track all your agricultural data</span>
                            </div>
                            <div class="feature-item">
                                <span class="feature-icon">🌱</span>
                                <span>Monitor crop growth and health from planting to harvest</span>
                            </div>
                            <div class="feature-item">
                                <span class="feature-icon">💧</span>
                                <span>Schedule and optimize irrigation based on soil and weather conditions</span>
                            </div>
                            <div class="feature-item">
                                <span class="feature-icon">📊</span>
                                <span>Get real-time analytics and actionable insights</span>
                            </div>
                            <div class="feature-item">
                                <span class="feature-icon">🛒</span>
                                <span>Buy and sell agricultural products in our marketplace</span>
                            </div>
                            <div class="feature-item">
                                <span class="feature-icon">🪙</span>
                                <span>Earn coins with every transaction and redeem them for discounts</span>
                            </div>
                        </div>
                        
                        <div style="text-align: center;">
                            <a href="%s/login" class="cta-button">Get Started Now →</a>
                        </div>
                        
                        <h3 style="color: #10b981;">Quick Tip:</h3>
                        <p style="background-color: #fffbeb; border-left: 4px solid #f59e0b; padding: 15px; border-radius: 4px;">
                            Start by adding your first farm in the dashboard. This will help you organize your crops and irrigation schedules more effectively.
                        </p>
                    </div>
                    
                    <div class="footer">
                        <p>If you have any questions or need assistance, feel free to reach out to our support team.</p>
                        <p style="margin-top: 15px; color: #999;">© 2026 FarmEazy. All rights reserved. | <a href="#" style="color: #10b981; text-decoration: none;">Support</a> | <a href="#" style="color: #10b981; text-decoration: none;">Contact</a></p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, getAppBaseUrl());
        
        sendHtmlEmail(userEmail, subject, htmlBody, EmailType.INFO);
        logger.info("Professional HTML welcome email sent to: {} (from: info@farm-eazy.com)", userEmail);
    }

    /**
     * Sends professional HTML password reset email.
     * 
     * @param userEmail User's email address
     * @param userName User's name
     * @param shortCode Short 8-character code for password reset
     */
    public void sendPasswordResetEmailHtml(String userEmail, String userName, String shortCode) {
        String subject = "Reset Your FarmEazy Password";
        String resetLink = getAppBaseUrl() + "/reset-password/" + shortCode;
        
        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 20px auto; background-color: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #ef4444 0%%, #dc2626 100%%); color: white; padding: 30px; text-align: center; }
                    .header h1 {
                        margin: 0;
                        font-size: 28px;
                    }
                    .content { padding: 30px; }
                    .alert-box {
                        background-color: #fef2f2;
                        border-left: 4px solid #ef4444;
                        padding: 15px;
                        margin: 20px 0;
                        border-radius: 4px;
                    }
                    .cta-button {
                        display: inline-block;
                        background: linear-gradient(135deg, #ef4444 0%%, #dc2626 100%%);
                        color: white;
                        padding: 14px 40px;
                        text-decoration: none;
                        border-radius: 4px;
                        margin: 20px 0;
                        font-weight: 600;
                        text-align: center;
                    }
                    .code-box {
                        background-color: #f3f4f6;
                        padding: 20px;
                        border-radius: 4px;
                        text-align: center;
                        font-family: 'Courier New', monospace;
                        font-size: 18px;
                        font-weight: bold;
                        color: #1f2937;
                        margin: 15px 0;
                    }
                    .footer { background-color: #f9fafb; padding: 20px; text-align: center; font-size: 12px; color: #666; border-top: 1px solid #e5e7eb; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔐 Password Reset Request</h1>
                    </div>
                    
                    <div class="content">
                        <p>Hello <strong>%s</strong>,</p>
                        
                        <p>We received a request to reset your FarmEazy password. If this wasn't you, you can ignore this email and your password will remain unchanged.</p>
                        
                        <div class="alert-box">
                            <strong>⚠️ Security Note:</strong> This link will expire in 1 hour for your security.
                        </div>
                        
                        <h3>To reset your password, click the button below:</h3>
                        <div style="text-align: center;">
                            <a href="%s" class="cta-button">Reset Password</a>
                        </div>
                        
                        <p style="text-align: center; color: #666;">Or use this code:</p>
                        <div class="code-box">%s</div>
                        
                        <p style="margin-top: 30px; font-size: 14px; color: #666;">
                            <strong>Didn't request this?</strong><br>
                            If you didn't request a password reset, your account is still secure. Please disregard this email.
                        </p>
                        
                        <table style="width: 100%%; margin-top: 20px; border-collapse: collapse;">
                            <tr style="background-color: #f3f4f6;">
                                <td style="padding: 10px; border: 1px solid #e5e7eb;">
                                    <strong>Account Email:</strong>
                                </td>
                                <td style="padding: 10px; border: 1px solid #e5e7eb;">
                                    %s
                                </td>
                            </tr>
                            <tr>
                                <td style="padding: 10px; border: 1px solid #e5e7eb;">
                                    <strong>Reset Link Valid Until:</strong>
                                </td>
                                <td style="padding: 10px; border: 1px solid #e5e7eb;">
                                    1 hour from now
                                </td>
                            </tr>
                        </table>
                    </div>
                    
                    <div class="footer">
                        <p>If you need help, contact our support team.</p>
                        <p style="margin-top: 15px; color: #999;">© 2026 FarmEazy. All rights reserved. | <a href="#" style="color: #ef4444; text-decoration: none;">Support</a></p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, resetLink, shortCode, userEmail);
        
        sendHtmlEmail(userEmail, subject, htmlBody, EmailType.SUPPORT);
        logger.info("Professional HTML password reset email sent to: {} (from: support@farm-eazy.com)", userEmail);
    }

    /**
     * Sends order confirmation email with detailed breakdown.
     * 
     * @param userEmail User's email address
     * @param userName User's name
     * @param orderId Order ID
     * @param totalAmount Total order amount
     */
    public void sendOrderConfirmationEmail(String userEmail, String userName, Long orderId, String totalAmount) {
        String subject = "Order Confirmed! 🎉 Your FarmEazy Order #" + orderId;
        
        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 20px auto; background-color: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #10b981 0%%, #059669 100%%); color: white; padding: 30px; text-align: center; }
                    .content { padding: 30px; }
                    .order-summary { background-color: #f0fdf4; border-left: 4px solid #10b981; padding: 20px; margin: 20px 0; border-radius: 4px; }
                    .footer { background-color: #f9fafb; padding: 20px; text-align: center; font-size: 12px; color: #666; border-top: 1px solid #e5e7eb; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>✅ Order Confirmed!</h1>
                        <p>Thank you for your purchase, %s</p>
                    </div>
                    
                    <div class="content">
                        <p>Your order has been successfully placed and is being processed.</p>
                        
                        <div class="order-summary">
                            <h3 style="margin-top: 0; color: #059669;">Order Details</h3>
                            <table style="width: 100%%;">
                                <tr>
                                    <td><strong>Order ID:</strong></td>
                                    <td>#FZ%s</td>
                                </tr>
                                <tr>
                                    <td><strong>Total Amount:</strong></td>
                                    <td><strong style="color: #10b981;">₹%s</strong></td>
                                </tr>
                                <tr>
                                    <td><strong>Expected Delivery:</strong></td>
                                    <td>3-5 business days</td>
                                </tr>
                                <tr>
                                    <td><strong>Order Status:</strong></td>
                                    <td><span style="background-color: #dbeafe; color: #0284c7; padding: 4px 8px; border-radius: 3px;">Processing</span></td>
                                </tr>
                            </table>
                        </div>
                        
                        <h3>What's Next?</h3>
                        <ul>
                            <li>You'll receive a shipping confirmation once your order ships</li>
                            <li>Track your order in your FarmEazy dashboard</li>
                            <li>Contact us if you have any questions</li>
                        </ul>
                    </div>
                    
                    <div class="footer">
                        <p>Thank you for shopping at FarmEazy! 🌾</p>
                        <p style="margin-top: 15px; color: #999;">© 2026 FarmEazy. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, orderId, totalAmount);
        
        sendHtmlEmail(userEmail, subject, htmlBody, EmailType.NOREPLY);
        logger.info("Order confirmation email sent to: {} for order: {} (from: no-reply@farm-eazy.com)", userEmail, orderId);
    }

    /**
     * Sends professional HTML product listing confirmation email.
     *
     * @param userEmail User's email address
     * @param userName User's name
     * @param productName Name of the listed product
     * @param category Product category
     * @param price Product price
     * @param quantity Product quantity
     * @param unit Product unit
     */
    public void sendProductListingConfirmationHtml(String userEmail, String userName, String productName, String category, Double price, Integer quantity, String unit) {
        String subject = "✅ Your Product is Live on FarmEazy!";
        
        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 20px auto; background-color: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #10b981 0%%, #059669 100%%); color: white; padding: 30px; text-align: center; }
                    .content { padding: 30px; }
                    .product-table { width: 100%%; border-collapse: collapse; margin: 20px 0; }
                    .product-table th, .product-table td { padding: 12px; border: 1px solid #e5e7eb; text-align: left; }
                    .product-table th { background-color: #f9fafb; }
                    .footer { background-color: #f9fafb; padding: 20px; text-align: center; font-size: 12px; color: #666; border-top: 1px solid #e5e7eb; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📦 Product Listed Successfully!</h1>
                    </div>
                    <div class="content">
                        <p>Hello <strong>%s</strong>,</p>
                        <p>Congratulations! Your product is now live on the FarmEazy marketplace and visible to thousands of potential buyers.</p>
                        
                        <h3 style="color: #059669;">Your Listing Details:</h3>
                        <table class="product-table">
                            <tr>
                                <th>Product Name</th>
                                <td>%s</td>
                            </tr>
                            <tr>
                                <th>Category</th>
                                <td>%s</td>
                            </tr>
                            <tr>
                                <th>Price</th>
                                <td>₹%.2f per %s</td>
                            </tr>
                            <tr>
                                <th>Available Quantity</th>
                                <td>%d %s</td>
                            </tr>
                        </table>

                        <h3 style="color: #059669;">What's Next?</h3>
                        <ul>
                            <li>Keep an eye out for inquiries from buyers.</li>
                            <li>You can manage your listings from your dashboard.</li>
                            <li>Consider sharing your listing on social media to increase visibility.</li>
                        </ul>
                    </div>
                    <div class="footer">
                        <p>Thank you for selling with FarmEazy! 🌾</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, productName, category, price, unit, quantity, unit);
        
        sendHtmlEmail(userEmail, subject, htmlBody, EmailType.INFO);
        logger.info("HTML Product listing confirmation email sent to: {} for product: {} (from: info@farm-eazy.com)", userEmail, productName);
    }

    /**
     * Sends a professional HTML generic notification email using INFO sender.
     *
     * @param userEmail User's email address
     * @param userName User's name
     * @param subject Email subject
     * @param message The main message content
     */
    private void sendNotificationHtml(String userEmail, String userName, String subject, String message) {
        sendNotificationHtml(userEmail, userName, subject, message, EmailType.INFO);
    }

    /**
     * Sends a professional HTML generic notification email with specific sender type.
     *
     * @param userEmail User's email address
     * @param userName User's name
     * @param subject Email subject
     * @param message The main message content
     * @param emailType The type of email (determines sender address)
     */
    private void sendNotificationHtml(String userEmail, String userName, String subject, String message, EmailType emailType) {
        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 20px auto; background-color: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #6b7280 0%%, #4b5563 100%%); color: white; padding: 30px; text-align: center; }
                    .content { padding: 30px; }
                    .greeting { font-size: 18px; }
                    .message-box { background-color: #f3f4f6; border-left: 4px solid #6b7280; padding: 20px; margin: 20px 0; border-radius: 4px; }
                    .footer { background-color: #f9fafb; padding: 20px; text-align: center; font-size: 12px; color: #666; border-top: 1px solid #e5e7eb; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>FarmEazy Notification</h1>
                    </div>
                    <div class="content">
                        <p class="greeting">Hello <strong>%s</strong>,</p>
                        <p>We have an update for you:</p>
                        <div class="message-box">
                            <h3 style="margin-top:0;">%s</h3>
                            <p>%s</p>
                        </div>
                        <p>You can always check the latest updates in your FarmEazy dashboard.</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated notification. Please do not reply to this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, subject, message);

        sendHtmlEmail(userEmail, subject, htmlBody, emailType);
        logger.info("HTML Notification email sent to: {} (from: {})", userEmail, emailType);
    }
}
