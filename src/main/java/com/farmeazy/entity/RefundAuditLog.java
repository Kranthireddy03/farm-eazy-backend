package com.farmeazy.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * REFUND AUDIT LOG ENTITY
 * 
 * Purpose: Track all refund-related actions for compliance and debugging.
 * Every refund status change, attempt, and completion is logged here.
 * 
 * @author FarmEazy Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "refund_audit_log")
public class RefundAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "action", length = 50, nullable = false)
    private String action;

    @Column(name = "previous_status", length = 30)
    private String previousStatus;

    @Column(name = "new_status", length = 30)
    private String newStatus;

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "razorpay_payment_id", length = 100)
    private String razorpayPaymentId;

    @Column(name = "razorpay_refund_id", length = 100)
    private String razorpayRefundId;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Constructors
    public RefundAuditLog() {
    }

    public RefundAuditLog(Order order, User user, String action) {
        this.order = order;
        this.user = user;
        this.action = action;
    }

    // Lifecycle callbacks
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // Static factory methods for common actions
    public static RefundAuditLog createRequestLog(Order order, User user, String reason) {
        RefundAuditLog log = new RefundAuditLog(order, user, "REFUND_REQUESTED");
        log.setPreviousStatus(Order.RefundStatus.NOT_REQUESTED.name());
        log.setNewStatus(Order.RefundStatus.REQUESTED.name());
        log.setNotes(reason);
        log.setAmount(order.getRefundAmount());
        return log;
    }

    public static RefundAuditLog createApprovalLog(Order order, User user, String notes) {
        RefundAuditLog log = new RefundAuditLog(order, user, "REFUND_APPROVED");
        log.setPreviousStatus(Order.RefundStatus.REQUESTED.name());
        log.setNewStatus(Order.RefundStatus.APPROVED.name());
        log.setNotes(notes);
        log.setAmount(order.getRefundAmount());
        return log;
    }

    public static RefundAuditLog createCompletionLog(Order order, User user, String refundId) {
        RefundAuditLog log = new RefundAuditLog(order, user, "REFUND_COMPLETED");
        log.setPreviousStatus(Order.RefundStatus.PROCESSING.name());
        log.setNewStatus(Order.RefundStatus.COMPLETED.name());
        log.setRazorpayRefundId(refundId);
        log.setAmount(order.getRefundAmount());
        return log;
    }

    public static RefundAuditLog createFailureLog(Order order, User user, String errorMessage) {
        RefundAuditLog log = new RefundAuditLog(order, user, "REFUND_FAILED");
        log.setPreviousStatus(Order.RefundStatus.PROCESSING.name());
        log.setNewStatus(Order.RefundStatus.FAILED.name());
        log.setNotes(errorMessage);
        log.setAmount(order.getRefundAmount());
        return log;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public String getRazorpayRefundId() {
        return razorpayRefundId;
    }

    public void setRazorpayRefundId(String razorpayRefundId) {
        this.razorpayRefundId = razorpayRefundId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
