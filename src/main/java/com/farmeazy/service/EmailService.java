package com.farmeazy.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
     * Uses NOREPLY sender for automated security alerts.
     * @param to Recipient email address
     * @param fullName User's full name
     */
    public void sendPasswordChangedConfirmation(String to, String fullName) {
        String subject = "Your FarmEazy password was changed";
        String body = "Hello " + fullName + ",\n\nYour password was changed successfully. If you did not perform this action, please contact support immediately at support@farm-eazy.com.\n\nRegards,\nFarmEazy Team";
        sendEmail(to, subject, body, EmailType.NOREPLY);
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
    String subject = "Irrigation Reminder: " + safeText(cropName, "Crop") + " at " + safeText(farmName, "Farm");
    String details = "Farm: " + safeText(farmName, "N/A") + "\n"
            + "Crop: " + safeText(cropName, "N/A") + "\n"
            + "Scheduled Time: " + safeText(scheduledTime, "N/A");

    String htmlBody = buildEventTemplateHtml(
            "Irrigation Reminder",
            "Hello " + safeText(userName, "User") + ", this is a reminder for your scheduled irrigation.",
            details,
            "Open Dashboard",
            getAppBaseUrl() + "/dashboard"
    );

    sendHtmlEmail(userEmail, subject, htmlBody, EmailType.INFO);
    logger.info("Template irrigation reminder email sent to: {}", userEmail);
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
    String subject = "Harvest Time Approaching for " + safeText(cropName, "Crop");
    String details = "Farm: " + safeText(farmName, "N/A") + "\n"
            + "Crop: " + safeText(cropName, "N/A") + "\n"
            + "Estimated Harvest Date: " + safeText(estimatedDate, "N/A");

    String htmlBody = buildEventTemplateHtml(
            "Harvest Notification",
            "Hello " + safeText(userName, "User") + ", your crop is nearing harvest time.",
            details,
            "List Produce",
            getAppBaseUrl() + "/products/new"
    );

    sendHtmlEmail(userEmail, subject, htmlBody, EmailType.INFO);
    logger.info("Template harvest notification email sent to: {}", userEmail);
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
     * Sends a professional HTML email for bank verification status updates.
     */
    public void sendBankVerificationStatusEmail(String userEmail,
                                                String userName,
                                                String verificationNumber,
                                                String amount,
                                                String referenceId,
                                                String maskedAccount,
                                                boolean success,
                                                String failureReason,
                                                String accountHolderName,
                                                String bankName,
                                                String ifscCode,
                                                LocalDateTime verificationTime,
                                                String verificationSource,
                                                String verificationStatus) {
        String normalizedStatus = normalizeVerificationStatus(success, verificationStatus, failureReason);
        String statusColor = resolveStatusColor(normalizedStatus);
        String statusMessage = resolveStatusMessage(normalizedStatus, failureReason);
        String statusImpact = resolveStatusImpact(normalizedStatus);
        String nextSteps = buildNextStepsHtml(normalizedStatus);

        String subject = "Bank Verification Status Update - " + safeText(verificationNumber, "N/A");
        String actionUrl = getAppBaseUrl() + "/bank-verification";
        String actionText = "Rejected".equals(normalizedStatus) ? "Update Bank Details"
                : "Pending".equals(normalizedStatus) ? "View Verification Status" : "Go to Dashboard";

        String rejectionBlock = "Rejected".equals(normalizedStatus)
                ? "<div style=\"margin-top:20px; font-size:13px; background:#fff5f5; padding:12px; border-left:4px solid #e53e3e;\">"
                + "<strong>Reason (if applicable):</strong><br>"
                + escapeHtml(safeText(failureReason, "The submitted bank account details could not be verified."))
                + "</div>"
                : "";

        String htmlBody = loadEmailTemplate("bank-verification-status.html")
                .replace("{{APP_NAME}}", "FarmEazy")
                .replace("{{USER_NAME}}", escapeHtml(safeText(userName, "Customer")))
                .replace("{{STATUS_COLOR}}", statusColor)
                .replace("{{STATUS}}", escapeHtml(normalizedStatus))
                .replace("{{STATUS_MESSAGE}}", escapeHtml(statusMessage))
                .replace("{{VERIFICATION_ID}}", escapeHtml(safeText(verificationNumber, "Not available")))
                .replace("{{VERIFICATION_TIME}}", escapeHtml(formatVerificationTime(verificationTime)))
                .replace("{{VERIFICATION_SOURCE}}", escapeHtml(safeText(verificationSource, "FarmEazy Verification System")))
                .replace("{{ACCOUNT_NAME}}", escapeHtml(safeText(accountHolderName, "Not available")))
                .replace("{{BANK_NAME}}", escapeHtml(safeText(bankName, "Not available")))
                .replace("{{MASKED_ACCOUNT_NUMBER}}", escapeHtml(safeText(maskedAccount, "Not available")))
                .replace("{{IFSC_CODE}}", escapeHtml(safeText(ifscCode, "Not available")))
                .replace("{{REJECTION_BLOCK}}", rejectionBlock)
                .replace("{{STATUS_IMPACT}}", escapeHtml(statusImpact))
                .replace("{{NEXT_STEPS}}", nextSteps)
                .replace("{{ACTION_URL}}", escapeHtml(actionUrl))
                .replace("{{ACTION_TEXT}}", escapeHtml(actionText))
                .replace("{{SUPPORT_EMAIL}}", escapeHtml(safeText(supportEmail, "support@farm-eazy.com")))
                .replace("{{WEBSITE_URL}}", escapeHtml(getAppBaseUrl()))
                .replace("{{YEAR}}", String.valueOf(LocalDate.now().getYear()));

        sendHtmlEmail(userEmail, subject, htmlBody, EmailType.NOREPLY);
    }

    public void sendServiceRequestConfirmationEmail(String userEmail,
                                                    String userName,
                                                    String requestId,
                                                    String serviceName,
                                                    LocalDateTime requestDate,
                                                    String schedule,
                                                    String status,
                                                    String serviceLocation,
                                                    String serviceDescription) {
        String subject = "Service Request Confirmation - " + safeText(requestId, "N/A");
        String appName = "FarmEazy";
        String htmlBody = loadEmailTemplate("service-request-confirmation.html")
                .replace("{{APP_NAME}}", appName)
                .replace("{{USER_NAME}}", escapeHtml(safeText(userName, "Customer")))
                .replace("{{REQUEST_ID}}", escapeHtml(safeText(requestId, "Not available")))
                .replace("{{SERVICE_NAME}}", escapeHtml(safeText(serviceName, "General Service")))
                .replace("{{REQUEST_DATE}}", escapeHtml(formatServiceRequestDate(requestDate)))
                .replace("{{SCHEDULE}}", escapeHtml(safeText(schedule, "As per availability")))
                .replace("{{STATUS}}", escapeHtml(safeText(status, "Submitted")))
                .replace("{{SERVICE_LOCATION}}", escapeHtml(safeText(serviceLocation, "Location details were not provided.")))
                .replace("{{SERVICE_DESCRIPTION}}", escapeHtml(safeText(serviceDescription, "No additional details provided.")))
                .replace("{{TRACK_REQUEST_URL}}", escapeHtml(getAppBaseUrl() + "/service-requests/" + safeText(requestId, "")))
                .replace("{{SUPPORT_EMAIL}}", escapeHtml(safeText(supportEmail, "support@farm-eazy.com")))
                .replace("{{WEBSITE_URL}}", escapeHtml(getAppBaseUrl()))
                .replace("{{YEAR}}", String.valueOf(LocalDate.now().getYear()));

        sendHtmlEmail(userEmail, subject, htmlBody, EmailType.NOREPLY);
    }

    public void sendServiceRequestSupportAlertEmail(String to,
                            String requestId,
                            String category,
                            String priority,
                            String subjectText,
                            String userName,
                            String userEmail,
                            String userPhone,
                            String description,
                            String relatedOrderId,
                            LocalDateTime submittedAt) {
    String subject = String.format("[%s] New Support Request: %s - %s",
        safeText(priority, "MEDIUM"), safeText(requestId, "N/A"), safeText(subjectText, "Support Request"));

    String htmlBody = loadEmailTemplate("service-request-support-alert.html")
        .replace("{{APP_NAME}}", "FarmEazy")
        .replace("{{REQUEST_ID}}", escapeHtml(safeText(requestId, "Not available")))
        .replace("{{CATEGORY}}", escapeHtml(safeText(category, "GENERAL")))
        .replace("{{PRIORITY}}", escapeHtml(safeText(priority, "MEDIUM")))
        .replace("{{REQUEST_SUBJECT}}", escapeHtml(safeText(subjectText, "Not available")))
        .replace("{{USER_NAME}}", escapeHtml(safeText(userName, "Not available")))
        .replace("{{USER_EMAIL}}", escapeHtml(safeText(userEmail, "Not available")))
        .replace("{{USER_PHONE}}", escapeHtml(safeText(userPhone, "N/A")))
        .replace("{{REQUEST_DESCRIPTION}}", escapeHtml(safeText(description, "No description provided.")))
        .replace("{{RELATED_ORDER_ID}}", escapeHtml(safeText(relatedOrderId, "N/A")))
        .replace("{{SUBMITTED_AT}}", escapeHtml(formatServiceRequestDate(submittedAt)))
        .replace("{{YEAR}}", String.valueOf(LocalDate.now().getYear()));

    sendHtmlEmail(to, subject, htmlBody, EmailType.SUPPORT);
    }

    public void sendServiceRequestStatusUpdateEmail(String to,
                            String userName,
                            String requestId,
                            String previousStatus,
                            String newStatus,
                            String resolutionNotes) {
    String subject = String.format("Service Request %s - Status Updated", safeText(requestId, "N/A"));

    String htmlBody = loadEmailTemplate("service-request-status-update.html")
        .replace("{{APP_NAME}}", "FarmEazy")
        .replace("{{USER_NAME}}", escapeHtml(safeText(userName, "Customer")))
        .replace("{{REQUEST_ID}}", escapeHtml(safeText(requestId, "Not available")))
        .replace("{{PREVIOUS_STATUS}}", escapeHtml(safeText(previousStatus, "Not available")))
        .replace("{{NEW_STATUS}}", escapeHtml(safeText(newStatus, "Not available")))
        .replace("{{RESOLUTION_NOTES}}", escapeHtml(safeText(resolutionNotes, "No additional notes provided.")))
        .replace("{{SUPPORT_EMAIL}}", escapeHtml(safeText(supportEmail, "support@farm-eazy.com")))
        .replace("{{YEAR}}", String.valueOf(LocalDate.now().getYear()));

    sendHtmlEmail(to, subject, htmlBody, EmailType.NOREPLY);
    }

    private String loadEmailTemplate(String templateName) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/emails/" + templateName);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to load email template: " + templateName, ex);
        }
    }

    
private String buildEventTemplateHtml(String title, String intro, String details, String actionText, String actionUrl) {
    String template = loadEmailTemplate("event-notification.html");
    return template
            .replace("{{APP_NAME}}", "FarmEazy")
            .replace("{{TITLE}}", escapeHtml(safeText(title, "Notification")))
            .replace("{{INTRO}}", escapeHtml(safeText(intro, "")))
            .replace("{{DETAILS}}", escapeHtml(safeText(details, "")).replace("\n", "<br>"))
            .replace("{{ACTION_TEXT}}", escapeHtml(safeText(actionText, "Open Dashboard")))
            .replace("{{ACTION_URL}}", escapeHtml(safeText(actionUrl, getAppBaseUrl() + "/dashboard")))
            .replace("{{YEAR}}", String.valueOf(LocalDate.now().getYear()));
}
private String normalizeVerificationStatus(boolean success, String verificationStatus, String failureReason) {
        String status = verificationStatus == null ? "" : verificationStatus.trim().toUpperCase();
        if ("PENDING".equals(status) || "INITIATED".equals(status) || "PROCESSING".equals(status)) {
            return "Pending";
        }
        if ("FAILED".equals(status) || "REJECTED".equals(status)) {
            return "Rejected";
        }
        if ("VERIFIED".equals(status) || "APPROVED".equals(status) || "SUCCESS".equals(status)) {
            return "Approved";
        }
        if (success) {
            return "Approved";
        }
        String reason = failureReason == null ? "" : failureReason.toLowerCase();
        if (reason.contains("pending") || reason.contains("under review")) {
            return "Pending";
        }
        return "Rejected";
    }

    private String resolveStatusColor(String status) {
        return switch (status) {
            case "Approved" -> "#2f855a";
            case "Pending" -> "#d69e2e";
            default -> "#c53030";
        };
    }

    private String resolveStatusMessage(String status, String failureReason) {
        return switch (status) {
            case "Approved" -> "Your bank account has been successfully verified. You can now proceed with transactions and withdrawals.";
            case "Pending" -> "Your bank verification is currently under review. No action is required at this stage.";
            default -> {
                String reason = safeText(failureReason, "The submitted bank account details could not be verified.");
                yield "Based on verification checks, your submission could not be approved. " + reason;
            }
        };
    }

    private String resolveStatusImpact(String status) {
        return switch (status) {
            case "Approved" -> "You can continue onboarding and use payout-related features without restrictions.";
            case "Pending" -> "Your verification is in progress. Payout-related actions may remain temporarily restricted until review is completed.";
            default -> "Payout and withdrawal actions will remain restricted until valid bank details are resubmitted and approved.";
        };
    }

    private String buildNextStepsHtml(String status) {
        List<String> steps = switch (status) {
            case "Approved" -> List.of(
                    "Proceed with payouts and withdrawal setup from your dashboard.",
                    "Retain this reference for future support communication."
            );
            case "Pending" -> List.of(
                    "No immediate action is required from your side.",
                    "Track verification status from your dashboard for updates."
            );
            default -> List.of(
                    "Review and correct account holder name, account number, and IFSC details.",
                    "Resubmit bank information from your dashboard for a fresh verification attempt."
            );
        };

        StringBuilder html = new StringBuilder();
        for (String step : steps) {
            html.append("<li>").append(escapeHtml(step)).append("</li>");
        }
        return html.toString();
    }

    private String formatVerificationTime(LocalDateTime verificationTime) {
        if (verificationTime == null) {
            return "Not available";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        return verificationTime.format(formatter);
    }

    private String formatServiceRequestDate(LocalDateTime requestDate) {
        if (requestDate == null) {
            return "Not available";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        return requestDate.format(formatter);
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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
    String subject = "Your FarmEazy Verification Code";
    String details = "Purpose: " + safeText(purpose, "Verification") + "\n"
            + "OTP Code: " + safeText(otpCode, "N/A") + "\n"
            + "Validity: 10 minutes";

    String htmlBody = buildEventTemplateHtml(
            "One-Time Password",
            "Hello " + safeText(userName, "User") + ", use this OTP to continue your action.",
            details,
            "Open Login",
            getAppBaseUrl() + "/login"
    );

    sendHtmlEmail(userEmail, subject, htmlBody, EmailType.NOREPLY);
    logger.info("Template OTP email sent to: {}", userEmail);
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
    String subject = "Welcome to FarmEazy";
    String details = "Account Holder: " + safeText(userName, "User") + "\n"
            + "Platform: FarmEazy Smart Farm Management";

    String htmlBody = buildEventTemplateHtml(
            "Welcome to FarmEazy",
            "Hello " + safeText(userName, "User") + ", your account is ready and you can start using FarmEazy.",
            details,
            "Get Started",
            getAppBaseUrl() + "/login"
    );

    sendHtmlEmail(userEmail, subject, htmlBody, EmailType.NOREPLY);
    logger.info("Template welcome email sent to: {}", userEmail);
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
    String resetLink = getAppBaseUrl() + "/reset-password/" + safeText(shortCode, "");
    String details = "Account: " + safeText(userEmail, "N/A") + "\n"
            + "Reset Code: " + safeText(shortCode, "N/A") + "\n"
            + "Link Validity: 1 hour";

    String htmlBody = buildEventTemplateHtml(
            "Password Reset Request",
            "Hello " + safeText(userName, "User") + ", we received a request to reset your password.",
            details,
            "Reset Password",
            resetLink
    );

    sendHtmlEmail(userEmail, subject, htmlBody, EmailType.NOREPLY);
    logger.info("Template password reset email sent to: {}", userEmail);
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
    String subject = "Order Confirmed! Your FarmEazy Order #" + safeText(orderId != null ? orderId.toString() : null, "N/A");
    String details = "Order ID: FZ" + safeText(orderId != null ? orderId.toString() : null, "N/A") + "\n"
            + "Total Amount: Rs " + safeText(totalAmount, "0.00") + "\n"
            + "Status: Processing";

    String htmlBody = buildEventTemplateHtml(
            "Order Confirmed",
            "Hello " + safeText(userName, "User") + ", your order has been placed successfully.",
            details,
            "Track Order",
            getAppBaseUrl() + "/orders"
    );

    sendHtmlEmail(userEmail, subject, htmlBody, EmailType.NOREPLY);
    logger.info("Template order confirmation email sent to: {}", userEmail);
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
    String subject = "Your Product is Live on FarmEazy";
    String details = "Product Name: " + safeText(productName, "N/A") + "\n"
            + "Category: " + safeText(category, "N/A") + "\n"
            + "Price: Rs " + (price != null ? String.format("%.2f", price) : "0.00") + " per " + safeText(unit, "unit") + "\n"
            + "Quantity: " + (quantity != null ? quantity : 0) + " " + safeText(unit, "unit");

    String htmlBody = buildEventTemplateHtml(
            "Product Listed Successfully",
            "Hello " + safeText(userName, "User") + ", your product listing is now visible to buyers.",
            details,
            "Manage Listings",
            getAppBaseUrl() + "/selling"
    );

    sendHtmlEmail(userEmail, subject, htmlBody, EmailType.INFO);
    logger.info("Template product listing confirmation email sent to: {}", userEmail);
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
    String htmlBody = buildEventTemplateHtml(
            safeText(subject, "FarmEazy Notification"),
            "Hello " + safeText(userName, "User") + ", we have an update for you.",
            safeText(message, "Please check your dashboard for details."),
            "Open Dashboard",
            getAppBaseUrl() + "/dashboard"
    );

    sendHtmlEmail(userEmail, subject, htmlBody, emailType);
    logger.info("Template notification email sent to: {}", userEmail);
}
}

