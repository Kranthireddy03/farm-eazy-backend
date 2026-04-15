package com.farmeazy.service;

import com.farmeazy.exception.EmailDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import com.farmeazy.dto.CommunicationPreferenceResponseDto;
import com.farmeazy.entity.CommunicationPreference;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * HTTP-based Email Service using Resend API
 * 
 * This service uses Resend's REST API instead of SMTP, which works
 * on platforms that block outbound SMTP connections (like Render free tier).
 * 
 * Features:
 * - Fast HTTP-based email delivery (no SMTP timeouts)
 * - Async methods for non-blocking operations
 * - 10-second timeout to prevent hanging
 * 
 * Setup:
 * 1. Sign up at https://resend.com
 * 2. Get your API key from the dashboard
 * 3. Set RESEND_API_KEY environment variable in Render
 * 
 * @author FarmEazy Development Team
 */
@Service
public class HttpEmailService {
                /**
                 * Legacy method for compatibility: sendNotificationEmail(String userEmail, String userName, String subject, String message)
                 */
                public boolean sendNotificationEmail(String userEmail, String userName, String subject, String message) {
                    // Default: use no-reply sender
                    return sendNoReplyMail(userEmail, subject, message);
                }

                /**
                 * Legacy method for compatibility: sendNotificationEmail(String userEmail, String userName, String subject, String message, EmailType emailType)
                 */
                public boolean sendNotificationEmail(String userEmail, String userName, String subject, String message, EmailType emailType) {
                    // Use sender based on EmailType
                    switch (emailType) {
                        case SUPPORT:
                            return sendSupportMail(userEmail, subject, message);
                        case INFO:
                            return sendInfoMail(userEmail, subject, message);
                        case NOREPLY:
                        default:
                            return sendNoReplyMail(userEmail, subject, message);
                    }
                }
            @Autowired
            private CommunicationPreferenceService communicationPreferenceService;
        private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT_LOGGER");
    @Autowired
    @Qualifier("noReplyMailSender")
    private JavaMailSender noReplySender;

    @Autowired
    @Qualifier("supportMailSender")
    private JavaMailSender supportSender;

    @Autowired(required = false)
    private CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Select appropriate JavaMailSender based on sender type.
     * Uses NOREPLY sender by default, and SUPPORT sender for support-related emails.
     */
    private JavaMailSender getJavaMailSender(SenderType senderType) {
        return senderType == SenderType.SUPPORT ? supportSender : noReplySender;
    }

    @Autowired
    @Qualifier("infoMailSender")
    private JavaMailSender infoSender;

    /**
     * Send notification email using no-reply sender
     */
    public boolean sendNoReplyMail(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom("no-reply@farm-eazy.com");
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            noReplySender.send(msg);
            return true;
        } catch (Exception e) {
            auditLogger.error("Failed to send no-reply mail", e);
            return false;
        }
    }

    /**
     * Send support email using support sender
     */
    public boolean sendSupportMail(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom("support@farm-eazy.com");
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            supportSender.send(msg);
            return true;
        } catch (Exception e) {
            auditLogger.error("Failed to send support mail", e);
            return false;
        }
    }

    /**
     * Send info email using info sender
     */
    public boolean sendInfoMail(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom("info@farm-eazy.com");
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            infoSender.send(msg);
            return true;
        } catch (Exception e) {
            auditLogger.error("Failed to send info mail", e);
            return false;
        }
    }

    // ...existing code...
    // --- STUBS FOR MISSING NOTIFICATION METHODS ---
    public boolean sendProductUpdateConfirmation(String userEmail, String userName, String productName, String category, Double price, Integer quantity, String unit) {
    String subject = "Product Updated Successfully - " + productName;
    String details = "Product Name: " + safeValue(productName) + "\n"
            + "Category: " + safeValue(category) + "\n"
            + "Price: Rs " + (price != null ? String.format("%.2f", price) : "N/A") + "\n"
            + "Quantity: " + (quantity != null ? quantity : 0) + " " + safeValue(unit);

    String html = buildEventEmailHtml(
            "Product Listing Updated",
            "Hello " + safeValue(userName) + ", your product listing was updated successfully.",
            details,
            "View My Listings",
            resolveEmailUrl("public", "/selling")
    );
    return sendEmail(userEmail, subject, html);
}

    public boolean sendProductDeleteConfirmation(String userEmail, String userName, String productName, String category, Double price, Integer quantity, String unit) {
    String subject = "Product Removed - " + productName;
    String details = "Product Name: " + safeValue(productName) + "\n"
            + "Category: " + safeValue(category) + "\n"
            + "Price: Rs " + (price != null ? String.format("%.2f", price) : "N/A") + "\n"
            + "Quantity: " + (quantity != null ? quantity : 0) + " " + safeValue(unit);

    String html = buildEventEmailHtml(
            "Product Listing Removed",
            "Hello " + safeValue(userName) + ", your product listing was removed from marketplace.",
            details,
            "Add New Product",
            resolveEmailUrl("public", "/selling")
    );
    return sendEmail(userEmail, subject, html);
}

    public boolean sendCoinEarnedNotification(String userEmail, String userName, Integer amount, Integer totalCoins, String reason) {
    String subject = "Coins Earned - FarmEazy";
        String template = loadEmailTemplate("templates/emails/coin-earned.html");
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("APP_NAME", "FarmEazy");
        placeholders.put("USER_NAME", safeValue(userName));
        placeholders.put("COINS_EARNED", String.valueOf(amount != null ? amount : 0));
        placeholders.put("REASON", safeValue(reason));
        placeholders.put("TOTAL_COINS", String.valueOf(totalCoins != null ? totalCoins : 0));
        placeholders.put("ACTIVITY_URL", resolveEmailUrl("public", "/activities"));
        placeholders.put("YEAR", String.valueOf(java.time.Year.now().getValue()));

        String html = replacePlaceholders(template, placeholders);
    return sendEmail(userEmail, subject, html);
}

    public boolean sendCoinSpentNotification(String userEmail, String userName, Integer amount, Integer totalCoins) {
    String subject = "Coins Used - FarmEazy";
        String template = loadEmailTemplate("templates/emails/coin-spent.html");
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("APP_NAME", "FarmEazy");
        placeholders.put("USER_NAME", safeValue(userName));
        placeholders.put("COINS_SPENT", String.valueOf(amount != null ? amount : 0));
        placeholders.put("DISCOUNT_VALUE", String.valueOf(amount != null ? amount : 0));
        placeholders.put("REMAINING_COINS", String.valueOf(totalCoins != null ? totalCoins : 0));
        placeholders.put("ACTIVITY_URL", resolveEmailUrl("public", "/activities"));
        placeholders.put("YEAR", String.valueOf(java.time.Year.now().getValue()));

        String html = replacePlaceholders(template, placeholders);
    return sendEmail(userEmail, subject, html);
}

    // ===========================================
    // REFUND NOTIFICATION EMAILS
    // ===========================================

    /**
     * Send professional refund success notification email
     */
    public boolean sendRefundSuccessNotification(String userEmail, String userName, Long orderId,
        Long coinsRefunded, java.math.BigDecimal amountRefunded, String refundId, String refundType) {
    String subject = "Refund Processed Successfully - Order #FZ" + orderId;
    auditLogger.info("EMAIL REQUEST: RefundSuccess | to={}, subject={}, orderId={}, coinsRefunded={}, amountRefunded={}, refundId={}, refundType={}", userEmail, subject, orderId, coinsRefunded, amountRefunded, refundId, refundType);

    String details = "Order: FZ" + (orderId != null ? orderId : 0) + "\n"
            + "Refund Type: " + safeValue(refundType) + "\n"
            + "Coins Refunded: " + (coinsRefunded != null ? coinsRefunded : 0) + "\n"
            + "Amount Refunded: Rs " + (amountRefunded != null ? amountRefunded : java.math.BigDecimal.ZERO) + "\n"
            + "Refund ID: " + safeValue(refundId);

    String html = buildEventEmailHtml(
            "Refund Successful",
            "Hello " + safeValue(userName) + ", your refund has been processed successfully.",
            details,
            "View Orders",
            resolveEmailUrl("public", "/orders")
    );
    return sendEmail(userEmail, subject, html);
}

    /**
     * Send refund requested notification email
     */
    public boolean sendRefundRequestedNotification(String userEmail, String userName, Long orderId,
        String refundType, String reason) {
    String subject = "Refund Request Received - Order #FZ" + orderId;
    auditLogger.info("EMAIL REQUEST: RefundRequested | to={}, subject={}, orderId={}, refundType={}, reason={}", userEmail, subject, orderId, refundType, reason);

    String details = "Order: FZ" + (orderId != null ? orderId : 0) + "\n"
            + "Request Type: " + safeValue(refundType) + "\n"
            + "Status: Processing\n"
            + "Reason: " + safeValue(reason);

    String html = buildEventEmailHtml(
            "Refund Request Received",
            "Hello " + safeValue(userName) + ", we have received your refund request and started processing.",
            details,
            "Track Request",
            resolveEmailUrl("public", "/orders")
    );
    return sendEmail(userEmail, subject, html);
}

    /**
     * Send refund failed notification email
     */
    public boolean sendRefundFailedNotification(String userEmail, String userName, Long orderId, String errorMessage) {
    String subject = "Refund Issue - Order #FZ" + orderId;
    String details = "Order: FZ" + (orderId != null ? orderId : 0) + "\n"
            + "Issue: " + safeValue(errorMessage) + "\n"
            + "Action: Please contact support if this persists.";

    String html = buildEventEmailHtml(
            "Refund Processing Issue",
            "Hello " + safeValue(userName) + ", we encountered an issue while processing your refund.",
            details,
            "Contact Support",
            resolveEmailUrl("support", "/support")
    );
    return sendEmail(userEmail, subject, html);
}

    /**
     * Send order cancellation confirmation email
     */
    public boolean sendOrderCancellationNotification(String userEmail, String userName, Long orderId,
        String reason, java.math.BigDecimal refundAmount, Long coinsToRefund, boolean refundDetailsRequired) {
    String subject = "Order Cancelled - #FZ" + orderId;

    String details = "Order: FZ" + (orderId != null ? orderId : 0) + "\n"
            + "Reason: " + safeValue(reason) + "\n"
            + "Refund Amount: Rs " + (refundAmount != null ? refundAmount : java.math.BigDecimal.ZERO) + "\n"
            + "Coins Refund: " + (coinsToRefund != null ? coinsToRefund : 0) + "\n"
            + "Refund Details Required: " + (refundDetailsRequired ? "Yes" : "No");

    String html = buildEventEmailHtml(
            "Order Cancelled",
            "Hello " + safeValue(userName) + ", your order has been cancelled as requested.",
            details,
            refundDetailsRequired ? "Add Refund Details" : "Continue Shopping",
            refundDetailsRequired ? resolveEmailUrl("public", "/refund-details") : resolveEmailUrl("public", "/buying")
    );
    return sendEmail(userEmail, subject, html);
}

    /**
     * Send return request confirmation email
     */
    public boolean sendReturnRequestNotification(String userEmail, String userName, Long orderId,
        String reason, java.math.BigDecimal refundAmount, Long coinsToRefund) {
    String subject = "Return Request Received - Order #FZ" + orderId;

    String details = "Order: FZ" + (orderId != null ? orderId : 0) + "\n"
            + "Reason: " + safeValue(reason) + "\n"
            + "Expected Refund Amount: Rs " + (refundAmount != null ? refundAmount : java.math.BigDecimal.ZERO) + "\n"
            + "Expected Coins: " + (coinsToRefund != null ? coinsToRefund : 0);

    String html = buildEventEmailHtml(
            "Return Request Received",
            "Hello " + safeValue(userName) + ", your return request has been received.",
            details,
            "Track Return",
            resolveEmailUrl("public", "/orders")
    );
    return sendEmail(userEmail, subject, html);
}

    public boolean sendServiceListingCreatedNotification(String userEmail, String userName, String title, Double rate, String description) {
    String subject = "Service Listed Successfully - " + title;
    String details = "Service Title: " + safeValue(title) + "\n"
            + "Rate: Rs " + (rate != null ? String.format("%.2f", rate) : "N/A") + " per hour\n"
            + "Description: " + safeValue(description);

    String html = buildEventEmailHtml(
            "Service Listing Created",
            "Hello " + safeValue(userName) + ", your service is now live on FarmEazy.",
            details,
            "Manage Services",
            resolveEmailUrl("public", "/irrigation-services")
    );
    return sendEmail(userEmail, subject, html);
}

    public boolean sendServiceListingUpdatedNotification(String userEmail, String userName, String title, Double rate, String description) {
    String subject = "Service Updated - " + title;
    String details = "Service Title: " + safeValue(title) + "\n"
            + "Rate: Rs " + (rate != null ? String.format("%.2f", rate) : "N/A") + " per hour\n"
            + "Description: " + safeValue(description);

    String html = buildEventEmailHtml(
            "Service Listing Updated",
            "Hello " + safeValue(userName) + ", your service listing details were updated.",
            details,
            "View Service",
            resolveEmailUrl("public", "/irrigation-services")
    );
    return sendEmail(userEmail, subject, html);
}

    public boolean sendServiceListingDeletedNotification(String userEmail, String userName, String title, Double rate, String description) {
    String subject = "Service Removed - " + title;
    String details = "Service Title: " + safeValue(title) + "\n"
            + "Rate: Rs " + (rate != null ? String.format("%.2f", rate) : "N/A") + " per hour\n"
            + "Description: " + safeValue(description);

    String html = buildEventEmailHtml(
            "Service Listing Removed",
            "Hello " + safeValue(userName) + ", your service listing has been removed.",
            details,
            "Add New Service",
            resolveEmailUrl("public", "/irrigation-services")
    );
    return sendEmail(userEmail, subject, html);
}

    public boolean sendServiceBookingApprovedNotification(String userEmail, String userName, String serviceType, String location, String providerName) {
    String subject = "Service Booking Approved - FarmEazy";
    String details = "Service Type: " + safeValue(serviceType) + "\n"
            + "Location: " + safeValue(location) + "\n"
            + "Provider: " + safeValue(providerName) + "\n"
            + "Status: Approved";

    String html = buildEventEmailHtml(
            "Service Booking Approved",
            "Hello " + safeValue(userName) + ", your service booking request has been approved.",
            details,
            "View Bookings",
            resolveEmailUrl("public", "/irrigation-services")
    );

    return sendEmail(userEmail, subject, html);
}

    public boolean sendServiceBookingDeclinedNotification(String userEmail, String userName, String serviceType, String location, String providerName) {
    String subject = "Service Booking Update - FarmEazy";
    String details = "Service Type: " + safeValue(serviceType) + "\n"
            + "Location: " + safeValue(location) + "\n"
            + "Provider: " + safeValue(providerName) + "\n"
            + "Status: Declined";

    String html = buildEventEmailHtml(
            "Service Booking Declined",
            "Hello " + safeValue(userName) + ", the requested booking could not be accommodated at this time.",
            details,
            "Browse Services",
            resolveEmailUrl("public", "/irrigation-services")
    );

    return sendEmail(userEmail, subject, html);
}
    // --- END STUBS ---

    private static final Logger logger = LoggerFactory.getLogger(HttpEmailService.class);

    private String getTraceId() {
        String traceId = org.slf4j.MDC.get("traceId");
        return traceId != null ? traceId : "<no-trace>";
    }
    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private static final int CONNECT_TIMEOUT_MS = 5000;  // 5 seconds
    private static final int READ_TIMEOUT_MS = 10000;    // 10 seconds

    @Value("${resend.api.key:}")
    private String resendApiKey;

    // Default from email (used when no specific type is provided)
    @Value("${resend.from.email:FarmEazy <no-reply@farm-eazy.com>}")
    private String fromEmail;

    // Multi-sender email addresses (domain verified in Resend - can use any @farm-eazy.com)
    @Value("${resend.from.noreply:FarmEazy <no-reply@farm-eazy.com>}")
    private String fromNoReply;

    @Value("${resend.from.info:FarmEazy Info <info@farm-eazy.com>}")
    private String fromInfo;

    @Value("${resend.from.support:FarmEazy Support <support@farm-eazy.com>}")
    private String fromSupport;

    @Value("${resend.from.orders:FarmEazy Orders <orders@farm-eazy.com>}")
    private String fromOrders;

    @Value("${farmeazy.mail.provider:resend}")
    private String mailProvider;

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

    private final RestTemplate restTemplate;

    /**
     * Email sender type enum for selecting appropriate from address
     */
    public enum SenderType {
        NOREPLY,  // Automated notifications, OTP, verifications - users should NOT reply
        INFO,     // Informational emails, welcome, product listings, updates
        SUPPORT,  // Support-related emails, password reset, bank issues, help
        ORDERS    // Order confirmations, shipping updates, invoices
    }

    /**
     * Get the appropriate sender email based on type
     */
    private String getSenderEmail(SenderType type) {
        return switch (type) {
            case NOREPLY -> fromNoReply;
            case INFO -> fromInfo;
            case SUPPORT -> fromSupport;
            case ORDERS -> fromOrders;
        };
    }

    public HttpEmailService() {
        // Configure RestTemplate with timeouts to prevent long waits
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Send email using Resend HTTP API with default sender (no-reply)
     * @throws EmailDeliveryException if email sending fails
     */
    public boolean sendEmail(String to, String subject, String htmlContent) {
        return sendEmail(to, subject, htmlContent, SenderType.NOREPLY);
    }

    /**
     * Send email using Resend HTTP API with specified sender type
     * @param to Recipient email address
     * @param subject Email subject
     * @param htmlContent HTML email body
     * @param senderType Type of sender (NOREPLY, INFO, SUPPORT, ORDERS)
     * @throws EmailDeliveryException if email sending fails
     */
    public boolean sendEmail(String to, String subject, String htmlContent, SenderType senderType) {
        String sender = getSenderEmail(senderType);
        String traceId = getTraceId();
        logger.info("[traceId={}] EMAIL SEND START | Primary=Zoho | SenderType={} | From={} | To={} | Subject={}",
            traceId, senderType, sender, maskEmail(to), subject);

        if (!emailEnabled) {
            logger.info("Email sending is disabled. Would have sent to: {} from: {}", maskEmail(to), sender);
            throw new EmailDeliveryException(
                "Email service is currently disabled. Please contact support.",
                "EMAIL_SERVICE_DISABLED",
                to
            );
        }

        if ("resend".equalsIgnoreCase(mailProvider)) {
            logger.info("[traceId={}] EMAIL SEND - Provider configured as RESEND. Skipping Zoho primary attempt.", traceId);
            return sendViaResend(to, subject, htmlContent, sender, traceId);
        }

        // ---- Primary Provider: Zoho SMTP (JavaMailSender) ----
        JavaMailSender mailSender = getJavaMailSender(senderType);
        logger.info("[traceId={}] EMAIL SEND (Zoho) - Using mail sender bean for {}", traceId, senderType);

        try {
            executeWithBreaker("emailPrimarySend", () -> {
                logger.info("[traceId={}] EMAIL SEND (Zoho) - Attempting SMTP send to {}", traceId, maskEmail(to));
                try {
                    jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
                    org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(mimeMessage, true, "UTF-8");
                    helper.setFrom(sender);
                    helper.setTo(to);
                    helper.setSubject(subject);
                    helper.setText(htmlContent, true);

                    mailSender.send(mimeMessage);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                return true;
            });
            logger.info("[traceId={}] EMAIL SEND (Zoho) - Success | To={} | From={}", traceId, maskEmail(to), sender);
            logger.info("[traceId={}] EMAIL PROVIDER: Zoho | Status=SUCCESS | Recipient={}", traceId, maskEmail(to));
            return true;
        } catch (Exception zohoEx) {
            logger.warn("[traceId={}] EMAIL SEND (Zoho) - Failed for {} | Error={}", traceId, maskEmail(to), zohoEx.getMessage(), zohoEx);
            logger.info("[traceId={}] EMAIL PROVIDER: Zoho | Status=FAIL | Reason={}", traceId, zohoEx.getMessage());
            logger.info("[traceId={}] EMAIL SEND - Falling back to Resend for {}", traceId, maskEmail(to));
        }

        return sendViaResend(to, subject, htmlContent, sender, traceId);
    }

    private boolean sendViaResend(String to, String subject, String htmlContent, String sender, String traceId) {
        String apiKey = System.getenv("RESEND_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = resendApiKey;
        }
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("RESEND_API_KEY not configured. Email not sent to: {}", maskEmail(to));
            throw new EmailDeliveryException(
                    "Email service is not properly configured. Please contact support.",
                    "EMAIL_SERVICE_NOT_CONFIGURED",
                    to
            );
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

            logger.info("[traceId={}] EMAIL SEND (Resend) - Sending request to {} | To={} | Subject={}", traceId, RESEND_API_URL, maskEmail(to), subject);
            logger.debug("[traceId={}] EMAIL SEND (Resend) - Request payload: {}", traceId, buildSafeResendPayloadLog(sender, to, subject, htmlContent));

            ResponseEntity<String> response = executeWithBreaker("emailFallbackSend", () ->
                    restTemplate.exchange(
                            RESEND_API_URL,
                            HttpMethod.POST,
                            request,
                            String.class
                    )
            );

            logger.info("[traceId={}] EMAIL SEND (Resend) - Response Status: {} | To={}", traceId, response.getStatusCode(), maskEmail(to));
            logger.debug("[traceId={}] EMAIL SEND (Resend) - Response Body: {}", traceId, response.getBody());

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("[traceId={}] Email sent successfully to: {} from: {}", traceId, maskEmail(to), sender);
                logger.info("[traceId={}] EMAIL PROVIDER: Resend | Status=SUCCESS | Response={}", traceId, response.getBody());
                return true;
            }

            logger.error("[traceId={}] Failed to send email. Status: {}, Body: {}", traceId, response.getStatusCode(), response.getBody());
            logger.error("[traceId={}] EMAIL PROVIDER: Resend | Status=FAIL | Response={}", traceId, response.getBody());
            throw new EmailDeliveryException(
                    "Failed to send email. Please try again later.",
                    "EMAIL_DELIVERY_FAILED",
                    to
            );
        } catch (Exception resendEx) {
            logger.error("[traceId={}] EMAIL SEND (Resend) - Failed for {} | Error={}", traceId, maskEmail(to), resendEx.getMessage(), resendEx);
            throw new EmailDeliveryException(
                    "Unable to send email at this time. Please try again later.",
                    to,
                    resendEx
            );
        }
    }

    private <T> T executeWithBreaker(String breakerName, java.util.function.Supplier<T> action) {
        if (circuitBreakerRegistry == null) {
            return action.get();
        }
        try {
            return circuitBreakerRegistry.circuitBreaker(breakerName).executeSupplier(action::get);
        } catch (CallNotPermittedException ex) {
            throw new EmailDeliveryException(
                    "Email service is temporarily busy. Please retry shortly.",
                    "EMAIL_PROVIDER_THROTTLED",
                    (String) null
            );
        }
    }

    /**
     * Parse error message from Resend API response
     */
    private String parseResendErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return null;
        }
        try {
            // Simple JSON parsing for "message" field
            if (responseBody.contains("\"message\"")) {
                int start = responseBody.indexOf("\"message\":");
                if (start != -1) {
                    start = responseBody.indexOf("\"", start + 10) + 1;
                    int end = responseBody.indexOf("\"", start);
                    if (start > 0 && end > start) {
                        String message = responseBody.substring(start, end);
                        // Make the message user-friendly
                        if (message.contains("domain is not verified")) {
                            return "Email service configuration issue. Our team has been notified. Please try again later or contact support.";
                        }
                        return message;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not parse Resend error message: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Send welcome email to new user (async - does not block caller)
     */
    @Async
    public CompletableFuture<Boolean> sendWelcomeEmailAsync(String userEmail, String userName) {
        return sendWelcomeEmailAsync(userEmail, userName, userEmail, null, null, null);
    }

    @Async
    public CompletableFuture<Boolean> sendWelcomeEmailAsync(String userEmail, String userName,
                                                             String registeredEmail, String userId, String userMobile, String createdDate) {
        try {
            boolean result = sendWelcomeEmail(userEmail, userName, registeredEmail, userId, userMobile, createdDate);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            logger.error("Async welcome email failed for {}: {}", userEmail, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Send welcome email to new user
     * Uses INFO sender - informational email for new users
     */
    public boolean sendWelcomeEmail(String userEmail, String userName) {
        return sendWelcomeEmail(userEmail, userName, userEmail, null, null, null);
    }

    public boolean sendWelcomeEmail(String userEmail, String userName, String registeredEmail, String userId, String userMobile, String createdDate) {
        String subject = "Welcome to FarmEazy! ðŸŒ¾";
        String html = buildWelcomeEmailHtml(userName, registeredEmail, userId, userMobile, createdDate);
        return sendEmail(userEmail, subject, html, SenderType.INFO);
    }

    /**
     * Send password reset email (this one should be sync to ensure delivery before response)
     * Uses SUPPORT sender - user-initiated support request
     */
    public boolean sendPasswordResetEmail(String userEmail, String shortCode) {
        return sendPasswordResetEmail(userEmail, shortCode, null, null, null);
    }

    public boolean sendPasswordResetEmail(String userEmail, String shortCode,
                                          String ipAddress, String location, String deviceInfo) {
        String subject = "Reset Your FarmEazy Password";
        // Password reset for end users should always resolve against the public app domain.
        String resetLink = resolveEmailUrl("public", "/r/" + shortCode);
        String html = buildPasswordResetEmailHtml(resetLink, ipAddress, location, deviceInfo);
        return sendEmail(userEmail, subject, html, SenderType.SUPPORT);
    }

    /**
     * Send password changed confirmation email
     * Uses SUPPORT sender - security notification
     */
    public boolean sendPasswordChangedConfirmation(String userEmail, String userName) {
        String subject = "Your FarmEazy Password Has Been Changed";
        String html = buildPasswordChangedEmailHtml(userName);
        return sendEmail(userEmail, subject, html, SenderType.SUPPORT);
    }

    /**
     * Build password changed confirmation email HTML
     */
    private String buildPasswordChangedEmailHtml(String userName) {
    String details = "Account: " + safeValue(userName) + "\n"
            + "Event: Password changed successfully";

    return buildEventEmailHtml(
            "Password Changed Successfully",
            "Your FarmEazy password has been updated. If this was not you, contact support immediately.",
            details,
            "Contact Support",
            resolveEmailUrl("support", "/support")
    );
}

    /**
     * Build welcome email HTML
     */
    private String buildWelcomeEmailHtml(String userName, String registeredEmail, String userId, String userMobile, String createdDate) {
        String template = loadEmailTemplate("templates/emails/welcome.html");

        String requestTime = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        String normalizedUserName = (userName == null || userName.isBlank()) ? "User" : userName.trim();
        String normalizedRegisteredEmail = normalizeMetadata(registeredEmail);
        String normalizedUserId = normalizeMetadata(userId);
        String normalizedUserMobile = normalizeMetadata(userMobile);
        String normalizedCreatedDate = normalizeMetadata(createdDate);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("APP_NAME", "FarmEazy");
        placeholders.put("USER_NAME", normalizedUserName);
        placeholders.put("USER_ID", normalizedUserId);
        placeholders.put("USER_EMAIL", normalizedRegisteredEmail);
        placeholders.put("USER_MOBILE", normalizedUserMobile);
        placeholders.put("CREATED_DATE", normalizedCreatedDate);
        placeholders.put("LOGIN_URL", resolveEmailUrl("public", "/login"));
        placeholders.put("SUPPORT_EMAIL", "support@farm-eazy.com");
        placeholders.put("WEBSITE_URL", resolveEmailUrl("public", ""));
        placeholders.put("YEAR", String.valueOf(java.time.Year.now().getValue()));
        placeholders.put("WELCOME_DETAILS_ROWS", buildWelcomeDetailsRows(normalizedUserId, normalizedRegisteredEmail, normalizedUserMobile, normalizedCreatedDate));
        placeholders.put("REQUEST_TIME", requestTime);

        return replacePlaceholders(template, placeholders);
    }

    private String buildWelcomeDetailsRows(String userId, String userEmail, String userMobile, String createdDate) {
        StringBuilder rows = new StringBuilder();
        if (!userId.isBlank()) {
            rows.append(buildRequestDetailRow("User ID", userId));
        }
        if (!userEmail.isBlank()) {
            rows.append(buildRequestDetailRow("Registered Email", userEmail));
        }
        if (!userMobile.isBlank()) {
            rows.append(buildRequestDetailRow("Mobile Number", userMobile));
        }
        if (!createdDate.isBlank()) {
            rows.append(buildRequestDetailRow("Account Created On", createdDate));
        }
        return rows.toString();
    }

    /**
     * Build password reset email HTML
     */
    private String buildPasswordResetEmailHtml(String resetLink, String ipAddress, String location, String deviceInfo) {
        String template = loadEmailTemplate("templates/emails/password-reset.html");

        String requestTime = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        String normalizedIp = normalizeMetadata(ipAddress);
        String normalizedLocation = normalizeMetadata(location);
        String normalizedDevice = normalizeMetadata(deviceInfo);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("APP_NAME", "FarmEazy");
        placeholders.put("USER_NAME", "User");
        placeholders.put("RESET_LINK", resetLink);
        placeholders.put("RESET_LINK_EXPIRY_MINUTES", "60");
        placeholders.put("REQUEST_TIME", requestTime);
        placeholders.put("IP_ADDRESS", normalizedIp);
        placeholders.put("LOCATION", normalizedLocation);
        placeholders.put("DEVICE_INFO", normalizedDevice);
        placeholders.put("REQUEST_DETAILS_ROWS", buildRequestDetailsRows(requestTime, normalizedIp, normalizedLocation, normalizedDevice));
        placeholders.put("SUPPORT_EMAIL", "support@farm-eazy.com");
        placeholders.put("WEBSITE_URL", resolveEmailUrl("public", ""));
        placeholders.put("YEAR", String.valueOf(java.time.Year.now().getValue()));

        return replacePlaceholders(template, placeholders);
    }

    /**
     * Send general notification email (async - does not block caller)
     */
    @Async
    public CompletableFuture<Boolean> sendNotificationAsync(String userEmail, String userName, String subject, String message) {
        try {
            boolean result = sendNotification(userEmail, userName, subject, message);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            logger.error("Async notification email failed for {}: {}", userEmail, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Send general notification email
     * Uses NOREPLY sender - automated notification, users should not reply
     */
    public boolean sendNotification(String userEmail, String userName, String subject, String message) {
        String html = buildNotificationEmailHtml(userName, subject, message);
        return sendEmail(userEmail, subject, html, SenderType.NOREPLY);
    }

    /**
     * Send OTP email
     * Uses NOREPLY sender - automated verification code
     */
    public boolean sendOtpEmail(String userEmail, String userName, String otpCode, String purpose) {
        return sendOtpEmail(userEmail, userName, otpCode, purpose, null, null, null);
    }

    public boolean sendOtpEmail(String userEmail, String userName, String otpCode, String purpose,
                                String ipAddress, String location, String deviceInfo) {
        String subject = "Your FarmEazy OTP Code - " + purpose;
        String html = buildOtpEmailHtml(userName, otpCode, purpose, ipAddress, location, deviceInfo);
        return sendEmail(userEmail, subject, html, SenderType.NOREPLY);
    }

    /**
     * Send product listing confirmation email (async - does not block caller)
     */
    @Async
    public CompletableFuture<Boolean> sendProductListingConfirmationAsync(String userEmail, String userName, String productName,
                                                   String category, Double price, 
                                                   Integer quantity, String unit) {
        try {
            boolean result = sendProductListingConfirmation(userEmail, userName, productName, category, price, quantity, unit);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            logger.error("Async product listing email failed for {}: {}", userEmail, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Send product listing confirmation email
     */
    public boolean sendProductListingConfirmation(String userEmail, String userName, String productName,
                                                   String category, Double price, 
                                                   Integer quantity, String unit) {
        String subject = "Product Listed Successfully - " + productName;
        String html = buildProductListingEmailHtml(userName, productName, category, price, quantity, unit);
        return sendEmail(userEmail, subject, html);
    }

    /**
     * Send order confirmation email (async - does not block caller)
     */
    @Async
    public CompletableFuture<Boolean> sendOrderConfirmationEmailAsync(String userEmail, String userName, Long orderId, String subtotal, String coinsDiscount, String taxAmount, String finalAmount, String paymentMethod, String paymentStatus, String orderStatus) {
        return sendOrderConfirmationEmailAsync(userEmail, userName, orderId, subtotal, coinsDiscount, taxAmount, finalAmount, paymentMethod, paymentStatus, orderStatus, null, null, null, null);
    }

    @Async
    public CompletableFuture<Boolean> sendOrderConfirmationEmailAsync(String userEmail, String userName, Long orderId, String subtotal, String coinsDiscount, String taxAmount, String finalAmount, String paymentMethod, String paymentStatus, String orderStatus, String orderDate, String orderItemsHtml, String deliveryAddress, String trackOrderUrl) {
        try {
            boolean result = sendOrderConfirmationEmail(userEmail, userName, orderId, subtotal, coinsDiscount, taxAmount, finalAmount, paymentMethod, paymentStatus, orderStatus, orderDate, orderItemsHtml, deliveryAddress, trackOrderUrl);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            logger.error("Async order confirmation email failed for {}: {}", userEmail, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Send order confirmation email with detailed breakdown
     * Uses ORDERS sender - order-related communications
     */
    public boolean sendOrderConfirmationEmail(String userEmail, String userName, Long orderId, String subtotal, String coinsDiscount, String taxAmount, String finalAmount, String paymentMethod, String paymentStatus, String orderStatus) {
        return sendOrderConfirmationEmail(userEmail, userName, orderId, subtotal, coinsDiscount, taxAmount, finalAmount, paymentMethod, paymentStatus, orderStatus, null, null, null, null);
    }

    public boolean sendOrderConfirmationEmail(String userEmail, String userName, Long orderId, String subtotal, String coinsDiscount, String taxAmount, String finalAmount, String paymentMethod, String paymentStatus, String orderStatus, String orderDate, String orderItemsHtml, String deliveryAddress, String trackOrderUrl) {
        String normalizedPaymentMethod = paymentMethod == null ? "UNKNOWN" : paymentMethod;
        String normalizedPaymentStatus = paymentStatus == null ? "UNKNOWN" : paymentStatus;
        String normalizedOrderStatus = orderStatus == null ? "UNKNOWN" : orderStatus;
        String subject = "Order Update #FZ" + orderId + " - " + normalizedOrderStatus + " / " + normalizedPaymentStatus;
        String html = buildOrderConfirmationEmailHtml(userName, orderId, subtotal, coinsDiscount, taxAmount, finalAmount, normalizedPaymentMethod, normalizedPaymentStatus, normalizedOrderStatus, orderDate, orderItemsHtml, deliveryAddress, trackOrderUrl);
        return sendEmail(userEmail, subject, html, SenderType.ORDERS);
    }

    /**
     * Build notification email HTML
     */
    private String buildNotificationEmailHtml(String userName, String subject, String message) {
        String template = loadEmailTemplate("templates/emails/notification.html");

        String safeUserName = (userName == null || userName.isBlank()) ? "User" : userName.trim();
        String safeSubject = normalizeMetadata(subject);
        if (safeSubject.isBlank()) {
            safeSubject = "Important Account Update";
        }

        String normalizedMessage = normalizeMetadata(message);
        String displayMessage = normalizedMessage.isBlank()
                ? "Please check your FarmEazy dashboard for the latest updates."
                : normalizedMessage;
        String formattedMessage = displayMessage.replace("\n", "<br>");

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("APP_NAME", "FarmEazy");
        placeholders.put("USER_NAME", safeUserName);
        placeholders.put("NOTIFICATION_TITLE", safeSubject);
        placeholders.put("NOTIFICATION_SUBTITLE", "Here is an important update related to your FarmEazy account.");
        placeholders.put("NOTIFICATION_MESSAGE", formattedMessage);
        placeholders.put("HIGHLIGHT_LABEL", "Notification Summary");
        placeholders.put("HIGHLIGHT_TITLE", safeSubject);
        placeholders.put("HIGHLIGHT_MESSAGE", formattedMessage);
        placeholders.put("DETAIL_ROWS", buildNotificationDetailRows(safeSubject));
        placeholders.put("ACTION_TEXT", "Open Dashboard");
        placeholders.put("ACTION_URL", resolveEmailUrl("public", "/dashboard"));
        placeholders.put("SUPPORT_EMAIL", "support@farm-eazy.com");
        placeholders.put("YEAR", String.valueOf(java.time.Year.now().getValue()));

        return replacePlaceholders(template, placeholders);
    }

    private String buildNotificationDetailRows(String subject) {
        String sentAt = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

        StringBuilder rows = new StringBuilder();
        rows.append(buildNotificationDetailRow("Notification Type", normalizeMetadata(subject)));
        rows.append(buildNotificationDetailRow("Sent At", sentAt));

        return rows.toString();
    }

    private String buildNotificationDetailRow(String label, String value) {
        String normalizedValue = normalizeMetadata(value);
        if (normalizedValue.isBlank()) {
            return "";
        }

        return "<tr>"
                + "<td style=\"padding:10px 14px;border-bottom:1px solid #e5e7eb;color:#6b7280;font-size:13px;width:40%;\">" + label + "</td>"
                + "<td style=\"padding:10px 14px;border-bottom:1px solid #e5e7eb;color:#111827;font-size:13px;font-weight:600;\">" + normalizedValue + "</td>"
                + "</tr>";
    }

    /**
     * Build OTP email HTML
     */
    private String buildOtpEmailHtml(String userName, String otpCode, String purpose) {
        return buildOtpEmailHtml(userName, otpCode, purpose, null, null, null);
    }

    private String buildOtpEmailHtml(String userName, String otpCode, String purpose,
                                     String ipAddress, String location, String deviceInfo) {
        String template = loadEmailTemplate("templates/emails/login-otp.html");

        String safeUserName = (userName == null || userName.isBlank()) ? "User" : userName;
        String requestTime = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

        String normalizedIp = normalizeMetadata(ipAddress);
        String normalizedLocation = normalizeMetadata(location);
        String normalizedDevice = normalizeMetadata(deviceInfo);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("APP_NAME", "FarmEazy");
        placeholders.put("USER_NAME", safeUserName);
        placeholders.put("OTP_CODE", otpCode);
        placeholders.put("OTP_EXPIRY_MINUTES", "10");
        placeholders.put("REQUEST_TIME", requestTime);
        placeholders.put("IP_ADDRESS", normalizedIp);
        placeholders.put("LOCATION", normalizedLocation);
        placeholders.put("DEVICE_INFO", normalizedDevice);
        placeholders.put("REQUEST_DETAILS_ROWS", buildRequestDetailsRows(requestTime, normalizedIp, normalizedLocation, normalizedDevice));
        placeholders.put("LOGIN_URL", resolveEmailUrl("public", "/login"));
        placeholders.put("RESET_PASSWORD_URL", resolveEmailUrl("public", "/forgot-password"));
        placeholders.put("SUPPORT_EMAIL", "support@farm-eazy.com");
        placeholders.put("WEBSITE_URL", resolveEmailUrl("public", ""));
        placeholders.put("YEAR", String.valueOf(java.time.Year.now().getValue()));
        placeholders.put("OTP_PURPOSE", purpose == null ? "LOGIN" : purpose);

        logger.debug("OTP_EMAIL_TEMPLATE_PARAMS: {}", sanitizeTemplateParamsForLog(placeholders));

        return replacePlaceholders(template, placeholders);
    }

    
private String buildEventEmailHtml(String title, String intro, String details, String actionText, String actionUrl) {
    String template = loadEmailTemplate("templates/emails/event-notification.html");
    Map<String, String> placeholders = new HashMap<>();
    placeholders.put("APP_NAME", "FarmEazy");
    placeholders.put("TITLE", safeValue(title));
    placeholders.put("INTRO", safeValue(intro));
    placeholders.put("DETAILS", safeValue(details).replace("\n", "<br>"));
    placeholders.put("ACTION_TEXT", safeValue(actionText));
    placeholders.put("ACTION_URL", safeValue(actionUrl));
    placeholders.put("YEAR", String.valueOf(java.time.Year.now().getValue()));
    return replacePlaceholders(template, placeholders);
}

private String safeValue(String value) {
    if (value == null || value.isBlank()) {
        return "N/A";
    }
    return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
}
private String loadEmailTemplate(String classpathLocation) {
        try {
            org.springframework.core.io.ClassPathResource resource = new org.springframework.core.io.ClassPathResource(classpathLocation);
            byte[] bytes = org.springframework.util.StreamUtils.copyToByteArray(resource.getInputStream());
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            logger.error("Failed to load email template {}: {}", classpathLocation, ex.getMessage());
            throw new EmailDeliveryException(
                    "Email template could not be loaded.",
                    "EMAIL_TEMPLATE_LOAD_FAILED",
                    (String) null
            );
        }
    }

    private String replacePlaceholders(String template, Map<String, String> placeholders) {
        String rendered = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            rendered = rendered.replace("{{" + entry.getKey() + "}}", value);
        }
        return rendered;
    }

    private String normalizeMetadata(String value) {
        return (value == null || value.isBlank()) ? "" : value.trim();
    }

    private String buildRequestDetailsRows(String requestTime, String ipAddress, String location, String deviceInfo) {
        StringBuilder rows = new StringBuilder();
        rows.append(buildRequestDetailRow("Date & Time", requestTime));

        if (!ipAddress.isBlank()) {
            rows.append(buildRequestDetailRow("IP Address", ipAddress));
        }
        if (!location.isBlank()) {
            rows.append(buildRequestDetailRow("Location", location));
        }
        if (!deviceInfo.isBlank()) {
            rows.append(buildRequestDetailRow("Device", deviceInfo));
        }

        return rows.toString();
    }

    private String buildRequestDetailRow(String label, String value) {
        return "<tr>"
                + "<td style=\"padding:4px 0;\">" + label + ":</td>"
                + "<td style=\"padding:4px 0;\"><strong>" + value + "</strong></td>"
                + "</tr>";
    }

    private Map<String, Object> buildSafeResendPayloadLog(String sender, String to, String subject, String htmlContent) {
        Map<String, Object> safePayload = new HashMap<>();
        safePayload.put("from", sender);
        safePayload.put("to", List.of(maskEmail(to)));
        safePayload.put("subject", subject);
        safePayload.put("htmlLength", htmlContent == null ? 0 : htmlContent.length());
        safePayload.put("htmlHash", hashForLog(htmlContent));
        return safePayload;
    }

    private Map<String, String> sanitizeTemplateParamsForLog(Map<String, String> placeholders) {
        Map<String, String> safeParams = new HashMap<>();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() == null ? "" : entry.getValue();
            String upperKey = key.toUpperCase();

            if (upperKey.contains("URL")) {
                safeParams.put(key, sanitizeUrlForLog(value));
            } else if (upperKey.contains("OTP") || upperKey.contains("TOKEN") || upperKey.contains("PASSWORD")) {
                safeParams.put(key, "[HASH:" + hashForLog(value) + "]");
            } else if (upperKey.contains("EMAIL")) {
                safeParams.put(key, maskEmail(value));
            } else if (upperKey.contains("PHONE")) {
                safeParams.put(key, maskPhone(value));
            } else if (upperKey.contains("IP")) {
                safeParams.put(key, maskIp(value));
            } else {
                safeParams.put(key, truncateForLog(value));
            }
        }
        return safeParams;
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) {
            return "**@" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + domain;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return phone.substring(0, 2) + "****" + phone.substring(phone.length() - 2);
    }

    private String maskIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "***";
        }
        if (ip.contains(".")) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + ".***.***." + parts[3];
            }
        }
        return "[HASH:" + hashForLog(ip) + "]";
    }

    private String truncateForLog(String value) {
        if (value == null) {
            return "";
        }
        int max = 120;
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }

    private String sanitizeUrlForLog(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String sanitized = url.replaceAll("(?i)(token=)[^&]+", "$1***");
        sanitized = sanitized.replaceAll("(?i)(otp=)[^&]+", "$1***");
        return truncateForLog(sanitized);
    }

    private String hashForLog(String value) {
        if (value == null || value.isBlank()) {
            return "EMPTY";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            return "HASH_UNAVAILABLE";
        }
    }

    /**
     * Build product listing confirmation email HTML
     */
    private String buildProductListingEmailHtml(String userName, String productName, String category,
                                             Double price, Integer quantity, String unit) {
    String totalValue = (price != null && quantity != null) ? String.format("%.2f", price * quantity) : "0.00";
    String details = "Product Name: " + safeValue(productName) + "\n"
            + "Category: " + safeValue(category) + "\n"
            + "Price: Rs " + (price != null ? String.format("%.2f", price) : "0.00") + "\n"
            + "Quantity: " + (quantity != null ? quantity : 0) + " " + safeValue(unit) + "\n"
            + "Total Value: Rs " + totalValue;

    return buildEventEmailHtml(
            "Product Listed Successfully",
            "Hello " + safeValue(userName) + ", your product is now live on FarmEazy marketplace.",
            details,
            "Manage Listings",
            resolveEmailUrl("public", "/selling")
    );
}

    /**
     * Build order confirmation email HTML with detailed pricing breakdown
     */
    private String buildOrderConfirmationEmailHtml(String userName, Long orderId, String subtotal, String coinsDiscount, String taxAmount, String finalAmount, String paymentMethod, String paymentStatus, String orderStatus, String orderDate, String orderItemsHtml, String deliveryAddress, String trackOrderUrl) {
        String template = loadEmailTemplate("templates/emails/order-confirmation.html");

        String normalizedUserName = (userName == null || userName.isBlank()) ? "Customer" : userName.trim();
        String normalizedOrderId = orderId == null ? "" : String.valueOf(orderId);
        String normalizedOrderDate = normalizeMetadata(orderDate);
        if (normalizedOrderDate.isBlank()) {
            normalizedOrderDate = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        }

        String normalizedPaymentMethod = normalizeOrderLabel(paymentMethod);
        String normalizedOrderStatus = normalizeOrderLabel(orderStatus);
        String normalizedPaymentStatus = normalizeOrderLabel(paymentStatus);
        String normalizedDeliveryAddress = normalizeMetadata(deliveryAddress);
        String resolvedTrackUrl = normalizeMetadata(trackOrderUrl);
        if (resolvedTrackUrl.isBlank()) {
            resolvedTrackUrl = resolveEmailUrl("public", "/orders");
        }

        String detailsRows = buildOrderDetailsRows(normalizedOrderId, normalizedOrderDate, normalizedPaymentMethod, normalizedOrderStatus, normalizedPaymentStatus);
        String itemsRows = normalizeMetadata(orderItemsHtml);
        if (itemsRows.isBlank()) {
            itemsRows = "<tr><td colspan=\"4\" style=\"padding:10px; border:1px solid #e2e8f0; text-align:center; color:#6b7280;\">Order items are available in your dashboard.</td></tr>";
        }
        String totalsRows = buildOrderTotalRows(subtotal, coinsDiscount, taxAmount, finalAmount);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("APP_NAME", "FarmEazy");
        placeholders.put("USER_NAME", normalizedUserName);
        placeholders.put("ORDER_ID", normalizedOrderId);
        placeholders.put("ORDER_DATE", normalizedOrderDate);
        placeholders.put("PAYMENT_METHOD", normalizedPaymentMethod);
        placeholders.put("ORDER_STATUS", normalizedOrderStatus);
        placeholders.put("ORDER_INFO_ROWS", detailsRows);
        placeholders.put("ORDER_ITEMS", itemsRows);
        placeholders.put("ORDER_TOTAL_ROWS", totalsRows);
        placeholders.put("DELIVERY_ADDRESS", normalizedDeliveryAddress.isBlank() ? "Not available" : normalizedDeliveryAddress);
        placeholders.put("TRACK_ORDER_URL", resolvedTrackUrl);
        placeholders.put("SUPPORT_EMAIL", "support@farm-eazy.com");
        placeholders.put("WEBSITE_URL", resolveEmailUrl("public", ""));
        placeholders.put("YEAR", String.valueOf(java.time.Year.now().getValue()));

        return replacePlaceholders(template, placeholders);
    }

    private String buildOrderDetailsRows(String orderId, String orderDate, String paymentMethod, String orderStatus, String paymentStatus) {
        StringBuilder rows = new StringBuilder();
        if (!orderId.isBlank()) {
            rows.append(buildRequestDetailRow("Order ID", orderId));
        }
        if (!orderDate.isBlank()) {
            rows.append(buildRequestDetailRow("Order Date", orderDate));
        }
        if (!paymentMethod.isBlank()) {
            rows.append(buildRequestDetailRow("Payment Method", paymentMethod));
        }
        if (!orderStatus.isBlank()) {
            rows.append(buildRequestDetailRow("Order Status", orderStatus));
        }
        if (!paymentStatus.isBlank()) {
            rows.append(buildRequestDetailRow("Payment Status", paymentStatus));
        }
        return rows.toString();
    }

    private String buildOrderTotalRows(String subtotal, String coinsDiscount, String taxAmount, String finalAmount) {
        StringBuilder rows = new StringBuilder();
        rows.append("<tr><td align=\"right\" style=\"padding:6px 0;\">Subtotal:</td><td align=\"right\" style=\"padding:6px 0;\"><strong>â‚¹")
                .append(formatCurrencyAmount(subtotal))
                .append("</strong></td></tr>");

        String normalizedDiscount = normalizeMetadata(coinsDiscount);
        if (!normalizedDiscount.isBlank()) {
            java.math.BigDecimal discountValue = java.math.BigDecimal.ZERO;
            try {
                discountValue = new java.math.BigDecimal(normalizedDiscount);
            } catch (Exception ignored) {
            }
            if (discountValue.compareTo(java.math.BigDecimal.ZERO) > 0) {
                rows.append("<tr><td align=\"right\" style=\"padding:6px 0; color:#065f46;\">Coin Discount:</td><td align=\"right\" style=\"padding:6px 0; color:#065f46;\"><strong>- â‚¹")
                        .append(formatCurrencyAmount(normalizedDiscount))
                        .append("</strong></td></tr>");
            }
        }

        String normalizedCharges = normalizeMetadata(taxAmount);
        if (!normalizedCharges.isBlank()) {
            rows.append("<tr><td align=\"right\" style=\"padding:6px 0;\">Delivery Charges:</td><td align=\"right\" style=\"padding:6px 0;\"><strong>â‚¹")
                    .append(formatCurrencyAmount(normalizedCharges))
                    .append("</strong></td></tr>");
        }

        rows.append("<tr style=\"background:#f9fafb;\"><td align=\"right\" style=\"padding:8px 0;\"><strong>Total Amount:</strong></td><td align=\"right\" style=\"padding:8px 0;\"><strong>â‚¹")
                .append(formatCurrencyAmount(finalAmount))
                .append("</strong></td></tr>");
        return rows.toString();
    }

    private String formatCurrencyAmount(String amount) {
        String normalized = normalizeMetadata(amount).replace(",", "");
        if (normalized.isBlank()) {
            return "0.00";
        }
        try {
            return new java.math.BigDecimal(normalized).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        } catch (Exception ex) {
            return normalized;
        }
    }

    private String normalizeOrderLabel(String value) {
        String normalized = normalizeMetadata(value).replace('_', ' ').toLowerCase();
        if (normalized.isBlank()) {
            return "";
        }
        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }
}




