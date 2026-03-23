package com.farmeazy.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * UNIFIED EMAIL SERVICE
 * 
 * A single email service that can switch between providers via configuration.
 * Supports both Resend (HTTP API) and Zoho (SMTP) with a simple flag.
 * 
 * CONFIGURATION:
 * - farmeazy.mail.provider=resend  → Uses Resend HTTP API
 * - farmeazy.mail.provider=zoho    → Uses Zoho SMTP via JavaMailSender
 * 
 * FEATURES:
 * - Multi-sender support (NOREPLY, INFO, SUPPORT, ORDERS)
 * - Local test mode (logs emails instead of sending)
 * - Async email sending
 * - Unified API regardless of provider
 * 
 * USAGE:
 * @Autowired UnifiedEmailService emailService;
 * emailService.sendEmail("user@example.com", "Subject", "<html>...</html>", SenderType.INFO);
 * 
 * @author FarmEazy Development Team
 * @version 1.0.0
 */
@Service
public class UnifiedEmailService {

    private static final Logger logger = LoggerFactory.getLogger(UnifiedEmailService.class);
    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;

    /**
     * Email sender type - determines which email address is used
     */
    public enum SenderType {
        NOREPLY,  // Automated notifications, OTP, verifications - users should NOT reply
        INFO,     // Informational emails, welcome, product listings, updates
        SUPPORT,  // Support-related emails, password reset, bank issues, help
        ORDERS    // Order confirmations, shipping updates, invoices
    }

    // Provider configuration
    @Value("${farmeazy.mail.provider:resend}")
    private String provider;

    @Value("${farmeazy.mail.enabled:true}")
    private boolean emailEnabled;

    @Value("${farmeazy.mail.local-test-mode:false}")
    private boolean localTestMode;

    @Value("${farmeazy.app.support-base-url:${FARMEAZY_SUPPORT_BASE_URL:https://support.farm-eazy.com}}")
    private String supportAppBaseUrl;

    @Value("${farmeazy.app.public-base-url:${FARMEAZY_PUBLIC_BASE_URL:https://www.farm-eazy.com}}")
    private String publicAppBaseUrl;

    @Value("${farmeazy.app.base-url:${farmeazy.app.public-base-url:${FARMEAZY_PUBLIC_BASE_URL:https://www.farm-eazy.com}}}")
    private String appBaseUrl;

    private String resolveEmailUrl(String option, String path) {
        String base = null;
        if ("support".equalsIgnoreCase(option)) base = supportAppBaseUrl;
        else if ("public".equalsIgnoreCase(option)) base = publicAppBaseUrl;
        if (base == null || base.isBlank()) base = appBaseUrl;
        if (base == null || base.isBlank()) base = "https://www.farm-eazy.com";
        return base.replaceAll("/$", "") + path;
    }

    // Resend configuration
    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${resend.from.noreply:FarmEazy <no-reply@farm-eazy.com>}")
    private String resendNoReply;

    @Value("${resend.from.info:FarmEazy Info <info@farm-eazy.com>}")
    private String resendInfo;

    @Value("${resend.from.support:FarmEazy Support <support@farm-eazy.com>}")
    private String resendSupport;

    @Value("${resend.from.orders:FarmEazy Orders <orders@farm-eazy.com>}")
    private String resendOrders;

    // Zoho configuration
    @Value("${zoho.from.noreply:no-reply@farm-eazy.com}")
    private String zohoNoReply;

    @Value("${zoho.from.info:info@farm-eazy.com}")
    private String zohoInfo;

    @Value("${zoho.from.support:support@farm-eazy.com}")
    private String zohoSupport;

    @Value("${zoho.from.orders:orders@farm-eazy.com}")
    private String zohoOrders;

    @Autowired(required = false)
    @Qualifier("noReplyMailSender")
    private JavaMailSender noReplyMailSender;

    @Autowired(required = false)
    @Qualifier("supportMailSender")
    private JavaMailSender supportMailSender;

    @Autowired(required = false)
    @Qualifier("infoMailSender")
    private JavaMailSender infoMailSender;

    private final RestTemplate restTemplate;

    public UnifiedEmailService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Get current email provider
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Check if using Resend provider
     */
    public boolean isResendProvider() {
        return "resend".equalsIgnoreCase(provider);
    }

    /**
     * Check if using Zoho provider
     */
    public boolean isZohoProvider() {
        return "zoho".equalsIgnoreCase(provider);
    }

    /**
     * Get appropriate sender email based on type and provider
     */
    private String getSenderEmail(SenderType type) {
        if (isResendProvider()) {
            return switch (type) {
                case NOREPLY -> resendNoReply;
                case INFO -> resendInfo;
                case SUPPORT -> resendSupport;
                case ORDERS -> resendOrders;
            };
        } else {
            // Use sender type for Zoho
            return switch (type) {
                case NOREPLY -> zohoNoReply;
                case INFO -> zohoInfo;
                case SUPPORT -> zohoSupport;
                case ORDERS -> zohoOrders;
            };
        }
    }

    /**
     * Send email with default sender (NOREPLY)
     */
    public boolean sendEmail(String to, String subject, String htmlContent) {
        return sendEmail(to, subject, htmlContent, SenderType.NOREPLY);
    }

    /**
     * Send email with specified sender type
     */
    public boolean sendEmail(String to, String subject, String htmlContent, SenderType senderType) {
        String sender = getSenderEmail(senderType);

        if (!emailEnabled) {
            logger.info("[{}] Email disabled. Would send to: {} from: {}", provider.toUpperCase(), to, sender);
            return false;
        }

        // Local test mode - log instead of sending
        if (localTestMode) {
            logTestEmail(to, subject, htmlContent, sender);
            return true;
        }

        // Try Zoho first
        boolean zohoResult = sendViaZoho(to, subject, htmlContent, sender);
        if (zohoResult) {
            logger.info("UnifiedEmailService: Email sent via Zoho to {}", to);
            logger.info("UnifiedEmailService: Provider used = ZOHO for {}", to);
            return true;
        } else {
            logger.warn("UnifiedEmailService: Zoho failed for {}. Retrying with Resend...", to);
            boolean resendResult = sendViaResend(to, subject, htmlContent, sender);
            if (resendResult) {
                logger.info("UnifiedEmailService: Email sent via Resend to {}", to);
                logger.info("UnifiedEmailService: Provider used = RESEND for {}", to);
                return true;
            } else {
                logger.error("UnifiedEmailService: Both Zoho and Resend failed for {}", to);
                return false;
            }
        }
    }

    /**
     * Send email via Resend HTTP API
     */
    private boolean sendViaResend(String to, String subject, String htmlContent, String sender) {
        String apiKey = System.getenv("RESEND_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = resendApiKey;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("[RESEND] API key not configured. Email not sent to: {}", to);
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> emailData = new HashMap<>();
            emailData.put("from", sender);
            emailData.put("to", List.of(to));
            emailData.put("subject", subject);
            emailData.put("html", htmlContent);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(emailData, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                RESEND_API_URL,
                HttpMethod.POST,
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("[RESEND] Email sent to: {} from: {}", to, sender);
                return true;
            } else {
                logger.error("[RESEND] Failed. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                return false;
            }

        } catch (HttpClientErrorException e) {
            logger.error("[RESEND] API error for {}: {} - {}", to, e.getStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            logger.error("[RESEND] Error sending to {}: {}", to, e.getMessage());
            return false;
        }
    }

    /**
     * Send email via Zoho SMTP
     */
    private boolean sendViaZoho(String to, String subject, String htmlContent, String sender) {
        JavaMailSender selectedSender = null;
        if (sender.equalsIgnoreCase(zohoNoReply)) {
            selectedSender = noReplyMailSender;
        } else if (sender.equalsIgnoreCase(zohoSupport)) {
            selectedSender = supportMailSender;
        } else if (sender.equalsIgnoreCase(zohoInfo)) {
            selectedSender = infoMailSender;
        } else {
            // fallback
            selectedSender = noReplyMailSender;
        }
        if (selectedSender == null) {
            logger.error("[ZOHO] JavaMailSender not configured for sender {}. Check spring.mail.* properties.", sender);
            return false;
        }
        try {
            MimeMessage message = selectedSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(sender);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            selectedSender.send(message);
            logger.info("[ZOHO] Email sent to: {} from: {}", to, sender);
            return true;
        } catch (MessagingException e) {
            logger.error("[ZOHO] Failed to send to {}: {}", to, e.getMessage());
            return false;
        }
    }

    /**
     * Log email in test mode
     */
    private void logTestEmail(String to, String subject, String htmlContent, String sender) {
        logger.info("\n========== LOCAL TEST MODE - EMAIL ==========");
        logger.info("PROVIDER: {}", provider.toUpperCase());
        logger.info("FROM: {}", sender);
        logger.info("TO: {}", to);
        logger.info("SUBJECT: {}", subject);
        // Extract links for easy testing
        java.util.regex.Pattern linkPattern = java.util.regex.Pattern.compile("href=\"([^\"]+)\"");
        java.util.regex.Matcher matcher = linkPattern.matcher(htmlContent);
        while (matcher.find()) {
            logger.info("LINK: {}", matcher.group(1));
        }
        logger.info("==============================================\n");
    }

    // ==========================================
    // CONVENIENCE METHODS FOR COMMON EMAILS
    // ==========================================

    /**
     * Send welcome email (async)
     */
    @Async
    public CompletableFuture<Boolean> sendWelcomeEmailAsync(String userEmail, String userName) {
        try {
            boolean result = sendWelcomeEmail(userEmail, userName);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            logger.error("Async welcome email failed for {}: {}", userEmail, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Send welcome email
     */
    public boolean sendWelcomeEmail(String userEmail, String userName) {
        String subject = "Welcome to FarmEazy! 🌾";
        String html = buildWelcomeEmailHtml(userName);
        return sendEmail(userEmail, subject, html, SenderType.INFO);
    }

    /**
     * Send password reset email
     */
    public boolean sendPasswordResetEmail(String userEmail, String shortCode) {
        String subject = "Reset Your FarmEazy Password";
        String resetLink = resolveEmailUrl("support", "/r/" + shortCode);
        String html = buildPasswordResetEmailHtml(resetLink);
        return sendEmail(userEmail, subject, html, SenderType.SUPPORT);
    }

    /**
     * Send password changed confirmation
     */
    public boolean sendPasswordChangedConfirmation(String userEmail, String userName) {
        String subject = "Your FarmEazy Password Has Been Changed";
        String html = buildPasswordChangedEmailHtml(userName);
        return sendEmail(userEmail, subject, html, SenderType.SUPPORT);
    }

    /**
     * Send OTP email
     */
    public boolean sendOtpEmail(String userEmail, String userName, String otpCode, String purpose) {
        String subject = "Your FarmEazy OTP Code - " + purpose;
        String html = buildOtpEmailHtml(userName, otpCode, purpose);
        return sendEmail(userEmail, subject, html, SenderType.NOREPLY);
    }

    /**
     * Send notification email (async)
     */
    @Async
    public CompletableFuture<Boolean> sendNotificationAsync(String userEmail, String userName, String subject, String message) {
        try {
            boolean result = sendNotification(userEmail, userName, subject, message);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            logger.error("Async notification failed for {}: {}", userEmail, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Send notification email
     */
    public boolean sendNotification(String userEmail, String userName, String subject, String message) {
        String html = buildNotificationEmailHtml(userName, subject, message);
        return sendEmail(userEmail, subject, html, SenderType.NOREPLY);
    }

    /**
     * Send order confirmation email
     */
    public boolean sendOrderConfirmationEmail(String userEmail, String userName, Long orderId, 
            String subtotal, String coinsDiscount, String taxAmount, String finalAmount) {
        String subject = "Order Confirmed #FZ" + orderId + " - FarmEazy";
        String html = buildOrderConfirmationEmailHtml(userName, orderId, subtotal, coinsDiscount, taxAmount, finalAmount);
        return sendEmail(userEmail, subject, html, SenderType.ORDERS);
    }

    // ==========================================
    // EMAIL HTML BUILDERS
    // ==========================================

    private String buildWelcomeEmailHtml(String userName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #22c55e, #16a34a); padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .header h1 { color: white; margin: 0; font-size: 28px; }
                    .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; background: #22c55e; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 20px; color: #6b7280; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🌾 Welcome to FarmEazy!</h1>
                    </div>
                    <div class="content">
                        <h2>Hello, %s! 👋</h2>
                        <p>Thank you for joining FarmEazy - your smart farm management solution!</p>
                        <p>With FarmEazy, you can:</p>
                        <ul>
                            <li>🌱 Track your crops and their growth stages</li>
                            <li>💧 Manage irrigation schedules</li>
                            <li>🏡 Organize multiple farms</li>
                            <li>📊 Monitor farm activities</li>
                        </ul>
                        <a href="%s/login" class="button">Go to Dashboard</a>
                    </div>
                    <div class="footer">
                        <p>© 2026 FarmEazy. Smart Farm Management.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, appBaseUrl);
    }

    private String buildPasswordResetEmailHtml(String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #3b82f6, #1d4ed8); padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .header h1 { color: white; margin: 0; font-size: 24px; }
                    .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; background: #3b82f6; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .warning { background: #fef3c7; border-left: 4px solid #f59e0b; padding: 10px 15px; margin: 15px 0; }
                    .footer { text-align: center; margin-top: 20px; color: #6b7280; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔐 Password Reset Request</h1>
                    </div>
                    <div class="content">
                        <p>Hello,</p>
                        <p>We received a request to reset your FarmEazy password.</p>
                        <a href="%s" class="button">Reset Password</a>
                        <div class="warning">
                            <strong>⚠️ Important:</strong> This link expires in 1 hour.
                        </div>
                        <p>If you didn't request this, you can safely ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 FarmEazy. Smart Farm Management.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(resetLink);
    }

    private String buildPasswordChangedEmailHtml(String userName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #22c55e, #16a34a); padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .header h1 { color: white; margin: 0; font-size: 24px; }
                    .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }
                    .success-box { background: #dcfce7; border-left: 4px solid #22c55e; padding: 15px; margin: 20px 0; }
                    .warning-box { background: #fef3c7; border-left: 4px solid #f59e0b; padding: 15px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 20px; color: #6b7280; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔐 Password Changed Successfully</h1>
                    </div>
                    <div class="content">
                        <p>Hello, %s! 👋</p>
                        <div class="success-box">
                            <strong>✅ Your password has been successfully changed.</strong>
                        </div>
                        <div class="warning-box">
                            <strong>⚠️ If you didn't make this change:</strong>
                            <p>Contact support immediately at support@farm-eazy.com</p>
                        </div>
                    </div>
                    <div class="footer">
                        <p>© 2026 FarmEazy. Smart Farm Management.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName);
    }

    private String buildOtpEmailHtml(String userName, String otpCode, String purpose) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #8b5cf6, #6d28d9); padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .header h1 { color: white; margin: 0; font-size: 24px; }
                    .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; text-align: center; }
                    .otp-box { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; }
                    .otp-code { font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #6d28d9; }
                    .footer { text-align: center; margin-top: 20px; color: #6b7280; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔑 Verification Code</h1>
                    </div>
                    <div class="content">
                        <p>Hello, %s!</p>
                        <p>Your OTP for <strong>%s</strong>:</p>
                        <div class="otp-box">
                            <div class="otp-code">%s</div>
                        </div>
                        <p>This code expires in 10 minutes.</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 FarmEazy. Smart Farm Management.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, purpose, otpCode);
    }

    private String buildNotificationEmailHtml(String userName, String subject, String message) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #22c55e, #16a34a); padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .header h1 { color: white; margin: 0; font-size: 24px; }
                    .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }
                    .message-box { background: white; padding: 20px; border-radius: 8px; border-left: 4px solid #22c55e; }
                    .footer { text-align: center; margin-top: 20px; color: #6b7280; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🌾 %s</h1>
                    </div>
                    <div class="content">
                        <p>Hello, %s! 👋</p>
                        <div class="message-box">
                            <p>%s</p>
                        </div>
                    </div>
                    <div class="footer">
                        <p>© 2026 FarmEazy. Smart Farm Management.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(subject, userName, message);
    }

    private String buildOrderConfirmationEmailHtml(String userName, Long orderId, 
            String subtotal, String coinsDiscount, String taxAmount, String finalAmount) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #22c55e, #16a34a); padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .header h1 { color: white; margin: 0; font-size: 24px; }
                    .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }
                    .order-box { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; }
                    .order-id { font-size: 20px; font-weight: bold; color: #22c55e; }
                    .price-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #e5e7eb; }
                    .total-row { font-weight: bold; font-size: 18px; border-bottom: none; }
                    .footer { text-align: center; margin-top: 20px; color: #6b7280; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>✅ Order Confirmed!</h1>
                    </div>
                    <div class="content">
                        <p>Hello, %s! 👋</p>
                        <div class="order-box">
                            <p class="order-id">Order #FZ%d</p>
                            <div class="price-row"><span>Subtotal</span><span>₹%s</span></div>
                            <div class="price-row"><span>Coins Discount</span><span>-₹%s</span></div>
                            <div class="price-row"><span>Tax</span><span>₹%s</span></div>
                            <div class="price-row total-row"><span>Total</span><span>₹%s</span></div>
                        </div>
                        <p>Thank you for your order!</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 FarmEazy. Smart Farm Management.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, orderId, subtotal, coinsDiscount, taxAmount, finalAmount);
    }
}
