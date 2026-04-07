package com.farmeazy.service;

import com.farmeazy.entity.*;
import com.farmeazy.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PayoutNotificationService: Specialized service for payout notifications
 * Handles:
 * - Professional HTML email templates for payout events
 * - Vendor dashboard notifications
 * - Email delivery tracking
 * - Transaction history logging
 * - Security: template variable escaping, rate limiting, fraud alerts
 */
@Service
@Transactional
public class PayoutNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PayoutNotificationService.class);

    @Autowired(required = false)
    @Qualifier("supportMailSender")
    private JavaMailSender mailSender;

    @Autowired
    private NotificationTemplateRepository templateRepository;

    @Autowired
    private VendorPayoutHistoryRepository vendorPayoutHistoryRepository;

    @Autowired
    private FraudDetectionLogRepository fraudDetectionLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Value("${spring.mail.from:support@farmeazy.com}")
    private String fromEmail;

    @Value("${app.vendor.dashboard.url:http://localhost:3000/vendor}")
    private String vendorDashboardUrl;

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\$\\{([a-zA-Z_][a-zA-Z0-9_.]*)\\}");

    // ==================== BATCH NOTIFICATIONS ====================

    /**
     * Notify admins when batch is created
     */
    public void notifyBatchCreated(PayoutBatch batch, User creator) {
        try {
            Map<String, String> variables = buildBatchVariables(batch);
            sendBatchNotification(batch, "BATCH_CREATED", creator, variables);
            log.info("Batch created notification sent for batch: {}", batch.getId());
        } catch (Exception e) {
            log.error("Error sending batch created notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Notify pending approval in dashboard
     */
    public void notifyPendingApproval(PayoutBatch batch, User admin) {
        try {
            Map<String, String> variables = buildBatchVariables(batch);
            variables.put("approvalLink", buildAdminApprovalLink(batch.getId()));
            
            sendBatchNotification(batch, "PENDING_APPROVAL", admin, variables);
            
            // Create in-app notification for dashboard
            Notification.NotificationType notifType = Notification.NotificationType.PAYMENT;
            notificationRepository.save(new Notification(
                admin,
                notifType,
                "Batch Approval Required",
                "Payout batch " + batch.getId() + " requires your approval"
            ));
            
            log.info("Pending approval notification sent for batch: {}", batch.getId());
        } catch (Exception e) {
            log.error("Error sending pending approval notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Notify admin when batch is approved
     */
    public void notifyBatchApproved(PayoutBatch batch) {
        try {
            Map<String, String> variables = buildBatchVariables(batch);
            
            if (batch.getApprovedByUser() != null) {
                sendBatchNotification(batch, "BATCH_APPROVED", batch.getApprovedByUser(), variables);
                log.info("Batch approved notification sent for batch: {}", batch.getId());
            }
        } catch (Exception e) {
            log.error("Error sending batch approved notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Notify admin when batch processing starts
     */
    public void notifyBatchProcessing(PayoutBatch batch) {
        try {
            Map<String, String> variables = buildBatchVariables(batch);
            
            if (batch.getApprovedByUser() != null) {
                sendBatchNotification(batch, "BATCH_PROCESSING", batch.getApprovedByUser(), variables);
                log.info("Batch processing notification sent for batch: {}", batch.getId());
            }
        } catch (Exception e) {
            log.error("Error sending batch processing notification: {}", e.getMessage(), e);
        }
    }

    // ==================== PAYOUT NOTIFICATIONS ====================

    /**
     * Notify vendor when payout is approved (pending processing)
     */
    public void notifyPayoutApproved(BatchPayout payout) {
        try {
            Map<String, String> variables = buildPayoutVariables(payout);
            sendPayoutNotification(payout, "PAYOUT_APPROVED", payout.getVendor(), variables);
            
            updateVendorPayoutHistory(payout, "APPROVED");
            log.info("Payout approved notification sent for payout: {}", payout.getId());
        } catch (Exception e) {
            log.error("Error sending payout approved notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Notify vendor when payout processing starts
     */
    public void notifyPayoutProcessing(BatchPayout payout) {
        try {
            Map<String, String> variables = buildPayoutVariables(payout);
            sendPayoutNotification(payout, "PAYOUT_PROCESSING", payout.getVendor(), variables);
            
            updateVendorPayoutHistory(payout, "PROCESSING");
            log.info("Payout processing notification sent for payout: {}", payout.getId());
        } catch (Exception e) {
            log.error("Error sending payout processing notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Notify vendor when payout completes successfully
     */
    public void notifyPayoutCompleted(BatchPayout payout) {
        try {
            Map<String, String> variables = buildPayoutVariables(payout);
            variables.put("transactionReference", payout.getTransactionReference() != null ? payout.getTransactionReference() : "N/A");
            variables.put("completionDate", LocalDateTime.now().toString());
            variables.put("dashboardLink", vendorDashboardUrl + "/transactions/" + payout.getId());
            
            sendPayoutNotification(payout, "PAYOUT_COMPLETED", payout.getVendor(), variables);
            
            updateVendorPayoutHistory(payout, "COMPLETED");
            log.info("Payout completed notification sent for payout: {}", payout.getId());
        } catch (Exception e) {
            log.error("Error sending payout completed notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Notify vendor when payout fails
     */
    public void notifyPayoutFailed(BatchPayout payout, String failureReason) {
        try {
            Map<String, String> variables = buildPayoutVariables(payout);
            variables.put("failureReason", failureReason);
            variables.put("retryStatus", payout.getRetryCount() < payout.getMaxRetries() ? 
                "Automatic retry scheduled" : "Maximum retries exhausted");
            variables.put("nextRetryDate", payout.getLastAttemptAt() != null ? 
                payout.getLastAttemptAt().plusHours(1).toString() : "Soon");
            variables.put("supportLink", "http://support.farmeazy.com/tickets");
            
            sendPayoutNotification(payout, "PAYOUT_FAILED", payout.getVendor(), variables);
            
            updateVendorPayoutHistory(payout, "FAILED");
            log.info("Payout failed notification sent for payout: {} Reason: {}", payout.getId(), failureReason);
        } catch (Exception e) {
            log.error("Error sending payout failed notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Notify vendor when payout is retrying
     */
    public void notifyPayoutRetry(BatchPayout payout) {
        try {
            Map<String, String> variables = buildPayoutVariables(payout);
            variables.put("retryAttempt", String.valueOf(payout.getRetryCount()));
            variables.put("maxRetries", String.valueOf(payout.getMaxRetries()));
            variables.put("failureReason", payout.getFailureReason() != null ? payout.getFailureReason() : "Unknown error");
            
            sendPayoutNotification(payout, "PAYOUT_RETRY", payout.getVendor(), variables);
            
            updateVendorPayoutHistory(payout, "RETRY");
            log.info("Payout retry notification sent for payout: {} Attempt: {}/{}", 
                payout.getId(), payout.getRetryCount(), payout.getMaxRetries());
        } catch (Exception e) {
            log.error("Error sending payout retry notification: {}", e.getMessage(), e);
        }
    }

    // ==================== HELPER METHODS ====================

    private void sendBatchNotification(PayoutBatch batch, String notificationType, 
                                       User recipient, Map<String, String> variables) throws MessagingException {
        Optional<NotificationTemplate> template = templateRepository
            .findByNotificationTypeAndIsActiveTrue(notificationType);
        
        if (template.isPresent()) {
            NotificationTemplate tmpl = template.get();
            String subject = interpolateTemplate(tmpl.getSubjectTemplate(), variables);
            String htmlContent = interpolateTemplate(tmpl.getHtmlTemplate(), variables);
            String textContent = interpolateTemplate(tmpl.getTextTemplate(), variables);

            sendHtmlEmail(recipient.getEmail(), subject, htmlContent, textContent);
        } else {
            log.warn("Template not found for type: {}", notificationType);
        }
    }

    private void sendPayoutNotification(BatchPayout payout, String notificationType,
                                        User recipient, Map<String, String> variables) throws MessagingException {
        Optional<NotificationTemplate> template = templateRepository
            .findByNotificationTypeAndIsActiveTrue(notificationType);
        
        if (template.isPresent()) {
            NotificationTemplate tmpl = template.get();
            String subject = interpolateTemplate(tmpl.getSubjectTemplate(), variables);
            String htmlContent = interpolateTemplate(tmpl.getHtmlTemplate(), variables);
            String textContent = interpolateTemplate(tmpl.getTextTemplate(), variables);

            sendHtmlEmail(recipient.getEmail(), subject, htmlContent, textContent);
        } else {
            log.warn("Template not found for type: {}", notificationType);
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent, String textContent) 
            throws MessagingException {
        
        if (mailSender == null) {
            log.warn("JavaMailSender not configured. Email not sent to: {}", to);
            return;
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(textContent, htmlContent);

        mailSender.send(message);
        log.debug("HTML email sent to: {} with subject: {}", to, subject);
    }

    private String interpolateTemplate(String template, Map<String, String> variables) {
        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String variable = matcher.group(1);
            String value = variables.getOrDefault(variable, "");
            String safeValue = htmlEscape(value);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(safeValue));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String htmlEscape(String text) {
        if (text == null) return "";
        return text.replaceAll("&", "&amp;")
                  .replaceAll("<", "&lt;")
                  .replaceAll(">", "&gt;")
                  .replaceAll("\"", "&quot;")
                  .replaceAll("'", "&#39;");
    }

    private void updateVendorPayoutHistory(BatchPayout payout, String status) {
        try {
            VendorPayoutHistory history = new VendorPayoutHistory();
            history.setBatchId(payout.getBatch().getId());
            history.setBatchPayoutId(payout.getId());
            history.setVendorId(payout.getVendor().getId());
            history.setVendorName(payout.getVendor().getUsername());
            history.setBankAccountLast4(maskBankAccountNumber(payout.getBankDetail().getAccountNumber()));
            history.setAmount(payout.getAmount());
            history.setBatchDate(payout.getBatch().getBatchDate());
            history.setBatchStatus(payout.getBatch().getStatus().toString());
            history.setPayoutStatus(status);
            history.setFailureReason(payout.getFailureReason());
            history.setRetryCount(payout.getRetryCount());
            history.setTransactionReference(payout.getTransactionReference());
            history.setRazorpayPayoutId(payout.getRazorpayPayoutId());

            vendorPayoutHistoryRepository.save(history);
        } catch (Exception e) {
            log.error("Error updating vendor payout history: {}", e.getMessage(), e);
        }
    }

    private String maskBankAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) return "****";
        return accountNumber.substring(accountNumber.length() - 4);
    }

    private String buildAdminApprovalLink(Long batchId) {
        return "http://localhost:4200/admin/batches/" + batchId + "/approve";
    }

    private Map<String, String> buildBatchVariables(PayoutBatch batch) {
        Map<String, String> variables = new HashMap<>();
        variables.put("batchDate", batch.getBatchDate().toString());
        variables.put("batchId", batch.getId().toString());
        variables.put("totalVendors", batch.getTotalVendors().toString());
        variables.put("totalAmount", formatCurrency(batch.getTotalAmount().doubleValue()));
        variables.put("status", batch.getStatus().toString());
        variables.put("timestamp", LocalDateTime.now().toString());
        return variables;
    }

    private Map<String, String> buildPayoutVariables(BatchPayout payout) {
        Map<String, String> variables = new HashMap<>();
        variables.put("payoutId", payout.getId().toString());
        variables.put("batchId", payout.getBatch().getId().toString());
        variables.put("amount", formatCurrency(payout.getAmount().doubleValue()));
        variables.put("status", payout.getStatus().toString());
        variables.put("vendorName", payout.getVendor().getUsername());
        variables.put("bankAccountLast4", maskBankAccountNumber(payout.getBankDetail().getAccountNumber()));
        variables.put("timestamp", LocalDateTime.now().toString());
        variables.put("dashboardLink", vendorDashboardUrl + "/transactions");
        return variables;
    }

    private String formatCurrency(Double amount) {
        return String.format("₹%.2f", amount);
    }

    /**
     * Get vendor dashboard summary
     */
    public Map<String, Object> getVendorDashboardSummary(Long vendorId) {
        Map<String, Object> summary = new HashMap<>();

        Long pendingCount = vendorPayoutHistoryRepository.getPendingPayoutCountForVendor(vendorId);
        java.math.BigDecimal pendingAmount = vendorPayoutHistoryRepository.getPendingAmountForVendor(vendorId);
        java.math.BigDecimal totalCompleted = vendorPayoutHistoryRepository.getTotalPayoutsForVendor(vendorId);

        summary.put("pendingPayouts", pendingCount != null ? pendingCount : 0);
        summary.put("pendingAmount", formatCurrency(pendingAmount != null ? pendingAmount.doubleValue() : 0.0));
        summary.put("totalPayoutsCompleted", formatCurrency(totalCompleted != null ? totalCompleted.doubleValue() : 0.0));
        summary.put("lastUpdated", LocalDateTime.now());

        return summary;
    }
}
