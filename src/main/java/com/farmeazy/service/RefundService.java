package com.farmeazy.service;

import com.farmeazy.entity.Order;
import com.farmeazy.entity.Order.RefundStatus;
import com.farmeazy.entity.RefundAuditLog;
import com.farmeazy.entity.User;
import com.farmeazy.entity.UserCoins;
import com.farmeazy.entity.UserRefundDetails;
import com.farmeazy.repository.OrderRepository;
import com.farmeazy.repository.RefundAuditLogRepository;
import com.farmeazy.repository.UserCoinsRepository;
import com.farmeazy.repository.UserRefundDetailsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for processing refunds via Razorpay.
 * Includes scheduled job for automatic refund processing.
 * 
 * @author FarmEazy Development Team
 */
@Service
@Transactional
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);
    private static final int MAX_REFUND_ATTEMPTS = 3;

    @Value("${farmeazy.refund.enabled:true}")
    private boolean refundEnabled;

    @Value("${farmeazy.refund.auto.process:true}")
    private boolean autoProcessEnabled;

    @Value("${razorpay.key.id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret:}")
    private String razorpayKeySecret;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRefundDetailsRepository refundDetailsRepository;

    @Autowired
    private RefundAuditLogRepository auditLogRepository;

    @Autowired
    private UserCoinsRepository userCoinsRepository;

    @Autowired
    private HttpEmailService emailService;

    /**
     * Scheduled job to process pending refunds.
     * Runs every 2 hours.
     */
    @Scheduled(cron = "0 0 */2 * * *") // Every 2 hours
    public void processScheduledRefunds() {
        LocalDateTime jobStartTime = LocalDateTime.now();
        log.info("BATCH_JOB_START: Refund processing job started at {}", jobStartTime);
        
        if (!refundEnabled || !autoProcessEnabled) {
            log.debug("Refund processing is disabled");
            LocalDateTime jobEndTime = LocalDateTime.now();
            log.info("BATCH_JOB_END: Refund processing skipped (disabled) at {}", jobEndTime);
            return;
        }

        int approvedCount = 0;
        int processedCount = 0;
        int retriedCount = 0;

        try {
            // First, auto-approve REQUESTED refunds
            approvedCount = approveRequestedRefunds();

            // Then, process APPROVED refunds
            processedCount = processApprovedRefunds();

            // Retry failed refunds
            retriedCount = retryFailedRefunds();

        } catch (Exception e) {
            log.error("Error in scheduled refund processing", e);
        }

        LocalDateTime jobEndTime = LocalDateTime.now();
        log.info("BATCH_JOB_END: Refund processing completed at {}. Approved: {}, Processed: {}, Retried: {}", 
                jobEndTime, approvedCount, processedCount, retriedCount);
    }

    /**
     * Auto-approve refund requests (can add admin approval flow later).
     * @return number of approved refunds
     */
    private int approveRequestedRefunds() {
        List<Order> requestedOrders = orderRepository.findByRefundStatus(RefundStatus.REQUESTED);
        int count = 0;

        for (Order order : requestedOrders) {
            try {
                order.setRefundStatus(RefundStatus.APPROVED);
                order.setRefundApprovedAt(LocalDateTime.now());
                orderRepository.save(order);

                // Create audit log
                RefundAuditLog auditLog = RefundAuditLog.createApprovalLog(
                        order, order.getUser(), "Auto-approved by system");
                auditLogRepository.save(auditLog);

                log.info("Auto-approved refund for order: {}", order.getId());
                count++;
            } catch (Exception e) {
                log.error("Error approving refund for order: {}", order.getId(), e);
            }
        }
        return count;
    }

    /**
     * Process approved refunds via Razorpay.
     * @return number of processed refunds
     */
    private int processApprovedRefunds() {
        List<Order> approvedOrders = orderRepository.findByRefundStatusAndRefundAttemptsLessThan(
                RefundStatus.APPROVED, MAX_REFUND_ATTEMPTS);

        for (Order order : approvedOrders) {
            processRefund(order);
        }
        return approvedOrders.size();
    }

    /**
     * Retry failed refunds (up to max attempts).
     * @return number of retried refunds
     */
    private int retryFailedRefunds() {
        List<Order> failedOrders = orderRepository.findFailedRefundsForRetry(MAX_REFUND_ATTEMPTS);

        for (Order order : failedOrders) {
            log.info("Retrying failed refund for order: {}, attempt: {}", 
                    order.getId(), order.getRefundAttempts() + 1);
            processRefund(order);
        }
        return failedOrders.size();
    }

    /**
     * Process a single refund.
     * Handles both coins refund and money refund.
     * - If only coins were used: refund coins only (no Razorpay call)
     * - If coins + money: refund both
     * - If only money: refund via Razorpay
     */
    public void processRefund(Order order) {
        log.info("Processing refund for order: {}, amount: {}, coinsUsed: {}", 
                order.getId(), order.getRefundAmount(), order.getCoinsUsed());

        try {
            // Update status to processing
            order.setRefundStatus(RefundStatus.PROCESSING);
            order.setRefundAttempts(order.getRefundAttempts() + 1);
            orderRepository.save(order);

            Long coinsToRefund = order.getCoinsUsed() != null ? order.getCoinsUsed() : 0L;
            BigDecimal amountToRefund = order.getRefundAmount() != null ? order.getRefundAmount() : BigDecimal.ZERO;
            String refundId = null;
            boolean coinsRefunded = false;
            boolean amountRefunded = false;

            // Step 1: Refund coins if any were used
            if (coinsToRefund > 0) {
                refundCoins(order.getUser(), coinsToRefund.intValue());
                order.setCoinsRefunded(coinsToRefund);
                coinsRefunded = true;
                log.info("Refunded {} coins to user: {}", coinsToRefund, order.getUser().getEmail());
            }

            // Step 2: Refund money if any amount was paid
            if (amountToRefund.compareTo(BigDecimal.ZERO) > 0 && order.getRazorpayPaymentId() != null) {
                // Get user's refund details for bank transfer
                Optional<UserRefundDetails> refundDetails = refundDetailsRepository.findByUser(order.getUser());
                if (refundDetails.isEmpty()) {
                    throw new IllegalStateException("User has no refund details for money refund");
                }

                // Process via Razorpay
                refundId = processRazorpayRefund(
                        order.getRazorpayPaymentId(),
                        amountToRefund,
                        refundDetails.get()
                );
                amountRefunded = true;
                log.info("Refunded ₹{} via Razorpay for order: {}", amountToRefund, order.getId());
            }

            // Update order with success
            order.setRefundStatus(RefundStatus.COMPLETED);
            if (refundId != null) {
                order.setRazorpayRefundId(refundId);
            }
            order.setRefundCompletedAt(LocalDateTime.now());
            order.setRefundErrorMessage(null);
            orderRepository.save(order);

            // Create audit log
            String auditNote = buildRefundAuditNote(coinsRefunded, coinsToRefund, amountRefunded, amountToRefund, refundId);
            RefundAuditLog auditLog = RefundAuditLog.createCompletionLog(order, order.getUser(), auditNote);
            auditLogRepository.save(auditLog);

            // Send success email
            sendRefundSuccessEmail(order, coinsRefunded, coinsToRefund, amountRefunded, amountToRefund);

            log.info("Refund completed for order: {}, refundId: {}", order.getId(), refundId);

        } catch (Exception e) {
            log.error("Refund failed for order: {}", order.getId(), e);

            // Update order with failure
            order.setRefundStatus(RefundStatus.FAILED);
            order.setRefundErrorMessage(e.getMessage());
            orderRepository.save(order);

            // Create audit log
            RefundAuditLog auditLog = RefundAuditLog.createFailureLog(order, order.getUser(), e.getMessage());
            auditLogRepository.save(auditLog);

            // Send failure email if max attempts reached
            if (order.getRefundAttempts() >= MAX_REFUND_ATTEMPTS) {
                sendRefundFailureEmail(order);
            }
        }
    }

    /**
     * Process refund via Razorpay API.
     * 
     * In production, this would call Razorpay's refund API:
     * POST https://api.razorpay.com/v1/payments/{paymentId}/refund
     * 
     * For now, simulates the refund process.
     */
    private String processRazorpayRefund(String paymentId, BigDecimal amount, UserRefundDetails refundDetails) {
        log.info("Calling Razorpay refund API for payment: {}, amount: {}", paymentId, amount);

        // Validate payment ID
        if (paymentId == null || paymentId.isEmpty()) {
            throw new IllegalArgumentException("Payment ID is required for refund");
        }

        // In production, call Razorpay API:
        /*
        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", amount.multiply(new BigDecimal("100")).intValue()); // Convert to paise
            refundRequest.put("speed", "normal"); // or "optimum" for instant
            refundRequest.put("notes", new JSONObject()
                .put("reason", "Customer request")
                .put("order_id", orderId)
            );
            
            // For bank transfer refunds (instead of refund to original payment):
            if (refundDetails.getPreferredMethod() == RefundMethod.BANK) {
                JSONObject bankAccount = new JSONObject();
                bankAccount.put("account_number", refundDetails.getAccountNumber());
                bankAccount.put("ifsc_code", refundDetails.getIfscCode());
                bankAccount.put("beneficiary_name", refundDetails.getAccountHolderName());
                refundRequest.put("bank_account", bankAccount);
            }
            
            Refund refund = client.payments.refund(paymentId, refundRequest);
            return refund.get("id");
            
        } catch (RazorpayException e) {
            throw new RuntimeException("Razorpay refund failed: " + e.getMessage(), e);
        }
        */

        // Simulate successful refund (for demo)
        // Returns a mock refund ID
        String mockRefundId = "rfnd_" + UUID.randomUUID().toString().substring(0, 14);
        log.info("Simulated Razorpay refund successful: {}", mockRefundId);
        return mockRefundId;
    }

    /**
     * Manually trigger refund for an order (admin action).
     */
    public void triggerManualRefund(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getRefundStatus() != RefundStatus.APPROVED && 
            order.getRefundStatus() != RefundStatus.FAILED) {
            throw new IllegalStateException("Order must be in APPROVED or FAILED status to manually process refund");
        }

        processRefund(order);
    }

    /**
     * Approve a refund request (admin action).
     */
    public void approveRefund(Long orderId, User adminUser, String notes) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getRefundStatus() != RefundStatus.REQUESTED && 
            order.getRefundStatus() != RefundStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Order must be in REQUESTED status to approve");
        }

        order.setRefundStatus(RefundStatus.APPROVED);
        order.setRefundApprovedAt(LocalDateTime.now());
        order.setRefundAdminNotes(notes);
        orderRepository.save(order);

        // Create audit log
        RefundAuditLog auditLog = RefundAuditLog.createApprovalLog(order, adminUser, notes);
        auditLogRepository.save(auditLog);

        log.info("Refund approved by admin for order: {}", orderId);
    }

    /**
     * Reject a refund request (admin action).
     */
    public void rejectRefund(Long orderId, User adminUser, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        order.setRefundStatus(RefundStatus.REJECTED);
        order.setRefundAdminNotes(reason);
        orderRepository.save(order);

        // Create audit log
        RefundAuditLog auditLog = new RefundAuditLog(order, adminUser, "REFUND_REJECTED");
        auditLog.setNotes(reason);
        auditLogRepository.save(auditLog);

        // Send rejection email
        try {
            emailService.sendNotificationEmail(
                    order.getUser().getEmail(),
                    order.getUser().getUsername(),
                    "Refund Request Update - FarmEazy",
                    String.format(
                            "Your refund request for order #FZ%d was not approved. Reason: %s. " +
                            "Please contact support if you have questions.",
                            order.getId(),
                            reason
                    )
            );
        } catch (Exception e) {
            log.error("Failed to send refund rejection email", e);
        }

        log.info("Refund rejected by admin for order: {}", orderId);
    }

    /**
     * Refund coins to user's account.
     */
    private void refundCoins(User user, int coinsToRefund) {
        Optional<UserCoins> userCoinsOpt = userCoinsRepository.findByUser(user);
        
        if (userCoinsOpt.isPresent()) {
            UserCoins userCoins = userCoinsOpt.get();
            userCoins.setTotalCoins(userCoins.getTotalCoins() + coinsToRefund);
            userCoins.setCoinsEarned(userCoins.getCoinsEarned() + coinsToRefund);
            // Reduce spent since we're refunding
            if (userCoins.getCoinsSpent() >= coinsToRefund) {
                userCoins.setCoinsSpent(userCoins.getCoinsSpent() - coinsToRefund);
            }
            userCoinsRepository.save(userCoins);
        } else {
            // Create new UserCoins if doesn't exist (shouldn't happen normally)
            UserCoins newUserCoins = new UserCoins(user, coinsToRefund);
            userCoinsRepository.save(newUserCoins);
        }
        
        log.info("Coins refunded to user {}: {} coins", user.getEmail(), coinsToRefund);
    }

    /**
     * Build audit note for refund completion.
     */
    private String buildRefundAuditNote(boolean coinsRefunded, Long coins, boolean amountRefunded, BigDecimal amount, String refundId) {
        StringBuilder note = new StringBuilder("Refund completed: ");
        
        if (coinsRefunded && coins != null && coins > 0) {
            note.append(coins).append(" coins refunded");
        }
        
        if (amountRefunded && amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            if (coinsRefunded) {
                note.append(" + ");
            }
            note.append("₹").append(amount).append(" refunded");
            if (refundId != null) {
                note.append(" (ID: ").append(refundId).append(")");
            }
        }
        
        return note.toString();
    }

    /**
     * Send refund success notification email (with coins support).
     */
    private void sendRefundSuccessEmail(Order order, boolean coinsRefunded, Long coins, boolean amountRefunded, BigDecimal amount) {
        try {
            String subject = "Refund Processed Successfully - FarmEazy";
            String userName = order.getUser().getUsername();
            String userEmail = order.getUser().getEmail();
            
            // Use professional email template
            emailService.sendRefundSuccessNotification(
                    userEmail,
                    userName,
                    order.getId(),
                    coinsRefunded ? coins : 0L,
                    amountRefunded ? amount : BigDecimal.ZERO,
                    order.getRazorpayRefundId(),
                    order.getRefundType() != null ? order.getRefundType().name() : "REFUND"
            );
        } catch (Exception e) {
            log.error("Failed to send refund success email", e);
        }
    }

    /**
     * Send refund success notification email (simple version for backwards compatibility).
     */
    private void sendRefundSuccessEmail(Order order) {
        try {
            emailService.sendNotificationEmail(
                    order.getUser().getEmail(),
                    order.getUser().getUsername(),
                    "Refund Processed Successfully - FarmEazy",
                    String.format(
                            "Great news! Your refund of ₹%.2f for order #FZ%d has been processed successfully. " +
                            "The amount will be credited to your account within 3-5 business days. " +
                            "Refund ID: %s",
                            order.getRefundAmount(),
                            order.getId(),
                            order.getRazorpayRefundId()
                    )
            );
        } catch (Exception e) {
            log.error("Failed to send refund success email", e);
        }
    }

    /**
     * Send refund failure notification email.
     */
    private void sendRefundFailureEmail(Order order) {
        try {
            emailService.sendRefundFailedNotification(
                    order.getUser().getEmail(),
                    order.getUser().getUsername(),
                    order.getId(),
                    order.getRefundErrorMessage() != null ? order.getRefundErrorMessage() : "Technical issue during processing"
            );
        } catch (Exception e) {
            log.error("Failed to send refund failure email", e);
        }
    }

    /**
     * Get refund statistics.
     */
    public RefundStats getRefundStats() {
        RefundStats stats = new RefundStats();
        stats.pendingCount = orderRepository.countByRefundStatus(RefundStatus.REQUESTED) +
                            orderRepository.countByRefundStatus(RefundStatus.APPROVED);
        stats.processingCount = orderRepository.countByRefundStatus(RefundStatus.PROCESSING);
        stats.completedCount = orderRepository.countByRefundStatus(RefundStatus.COMPLETED);
        stats.failedCount = orderRepository.countByRefundStatus(RefundStatus.FAILED);
        stats.rejectedCount = orderRepository.countByRefundStatus(RefundStatus.REJECTED);
        return stats;
    }

    /**
     * Statistics for refund dashboard.
     */
    public static class RefundStats {
        public long pendingCount;
        public long processingCount;
        public long completedCount;
        public long failedCount;
        public long rejectedCount;
    }
}
