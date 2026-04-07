package com.farmeazy.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity for individual payouts within a batch
 * Status lifecycle: PENDING → APPROVED → PROCESSING → COMPLETED/FAILED/RETRY
 * Used exclusively for batch payout system (separate from order payouts)
 */
@Entity
@Table(name = "batch_payout",
       uniqueConstraints = @UniqueConstraint(columnNames = {"batch_id", "vendor_id"}),
       indexes = {
           @Index(name = "idx_batch_payout_batch", columnList = "batch_id"),
           @Index(name = "idx_batch_payout_vendor", columnList = "vendor_id"),
           @Index(name = "idx_batch_payout_status", columnList = "status"),
           @Index(name = "idx_batch_payout_created_at", columnList = "created_at")
       })
public class BatchPayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private PayoutBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private User vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_detail_id", nullable = false)
    private UserBankDetails bankDetail;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayoutStatus status = PayoutStatus.PENDING;

    @Column(nullable = false)
    private Integer retryCount = 0;

    @Column(nullable = false)
    private Integer maxRetries = 3;

    @Column(length = 1000)
    private String failureReason;

    @Column
    private LocalDateTime lastAttemptAt;

    @Column(length = 100)
    private String transactionReference;

    @Column(length = 100)
    private String razorpayPayoutId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ===== CONSTRUCTORS =====
    public BatchPayout() {}

    public BatchPayout(Long id, PayoutBatch batch, User vendor, UserBankDetails bankDetail,
                      BigDecimal amount, PayoutStatus status, Integer retryCount, Integer maxRetries,
                      String failureReason, LocalDateTime lastAttemptAt, String transactionReference,
                      String razorpayPayoutId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.batch = batch;
        this.vendor = vendor;
        this.bankDetail = bankDetail;
        this.amount = amount;
        this.status = status;
        this.retryCount = retryCount;
        this.maxRetries = maxRetries;
        this.failureReason = failureReason;
        this.lastAttemptAt = lastAttemptAt;
        this.transactionReference = transactionReference;
        this.razorpayPayoutId = razorpayPayoutId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ===== GETTERS =====
    public Long getId() { return id; }
    public PayoutBatch getBatch() { return batch; }
    public User getVendor() { return vendor; }
    public UserBankDetails getBankDetail() { return bankDetail; }
    public BigDecimal getAmount() { return amount; }
    public PayoutStatus getStatus() { return status; }
    public Integer getRetryCount() { return retryCount; }
    public Integer getMaxRetries() { return maxRetries; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getLastAttemptAt() { return lastAttemptAt; }
    public String getTransactionReference() { return transactionReference; }
    public String getRazorpayPayoutId() { return razorpayPayoutId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ===== SETTERS =====
    public void setId(Long id) { this.id = id; }
    public void setBatch(PayoutBatch batch) { this.batch = batch; }
    public void setVendor(User vendor) { this.vendor = vendor; }
    public void setBankDetail(UserBankDetails bankDetail) { this.bankDetail = bankDetail; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setStatus(PayoutStatus status) { this.status = status; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public void setLastAttemptAt(LocalDateTime lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }
    public void setRazorpayPayoutId(String razorpayPayoutId) { this.razorpayPayoutId = razorpayPayoutId; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Status lifecycle for batch payout
     */
    public enum PayoutStatus {
        PENDING,      // Initial state - awaiting batch approval
        APPROVED,     // Batch approved, payout queued
        PROCESSING,   // Transfer initiated
        COMPLETED,    // Successfully transferred
        FAILED,       // Permanent failure (max retries exceeded)
        RETRY,        // Temporary failure - will retry
        CANCELLED     // Manually cancelled
    }

    /**
     * Check if payout can be processed
     */
    public boolean canProcess() {
        return this.status == PayoutStatus.APPROVED;
    }

    /**
     * Mark as completed
     */
    public void markCompleted(String transactionRef) {
        this.status = PayoutStatus.COMPLETED;
        this.transactionReference = transactionRef;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark as failed with retry logic
     */
    public void markFailed(String reason) {
        if (retryCount >= maxRetries) {
            this.status = PayoutStatus.FAILED;
        } else {
            this.status = PayoutStatus.RETRY;
            this.retryCount++;
        }
        this.failureReason = reason;
        this.lastAttemptAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
