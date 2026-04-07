package com.farmeazy.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * Vendor payout history denormalized view
 * Optimized for fast queries and vendor dashboard display
 */
@Entity
@Table(name = "vendor_payout_history",
       indexes = {
           @Index(name = "idx_vendor_id", columnList = "vendor_id"),
           @Index(name = "idx_batch_id", columnList = "batch_id"),
           @Index(name = "idx_payout_status", columnList = "payout_status"),
           @Index(name = "idx_created_at", columnList = "created_at"),
           @Index(name = "idx_batch_date", columnList = "batch_date")
       })
public class VendorPayoutHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long batchId;

    @Column(nullable = false)
    private Long batchPayoutId;

    @Column(nullable = false)
    private Long vendorId;

    @Column(length = 255)
    private String vendorName;

    @Column(length = 4)
    private String bankAccountLast4;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate batchDate;

    @Column(length = 50)
    private String batchStatus;  // CREATED, APPROVED, PROCESSING, COMPLETED, FAILED

    @Column(nullable = false, length = 50)
    private String payoutStatus;  // PENDING, APPROVED, PROCESSING, COMPLETED, FAILED, RETRY, CANCELLED

    @Column(length = 1000)
    private String failureReason;

    @Column
    private Integer retryCount = 0;

    @Column(length = 100)
    private String transactionReference;

    @Column(length = 100)
    private String razorpayPayoutId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ===== CONSTRUCTORS =====
    public VendorPayoutHistory() {}

    public VendorPayoutHistory(Long id, Long batchId, Long batchPayoutId, Long vendorId, String vendorName,
                              String bankAccountLast4, BigDecimal amount, LocalDate batchDate, String batchStatus,
                              String payoutStatus, String failureReason, Integer retryCount,
                              String transactionReference, String razorpayPayoutId,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.batchId = batchId;
        this.batchPayoutId = batchPayoutId;
        this.vendorId = vendorId;
        this.vendorName = vendorName;
        this.bankAccountLast4 = bankAccountLast4;
        this.amount = amount;
        this.batchDate = batchDate;
        this.batchStatus = batchStatus;
        this.payoutStatus = payoutStatus;
        this.failureReason = failureReason;
        this.retryCount = retryCount;
        this.transactionReference = transactionReference;
        this.razorpayPayoutId = razorpayPayoutId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ===== GETTERS =====
    public Long getId() { return id; }
    public Long getBatchId() { return batchId; }
    public Long getBatchPayoutId() { return batchPayoutId; }
    public Long getVendorId() { return vendorId; }
    public String getVendorName() { return vendorName; }
    public String getBankAccountLast4() { return bankAccountLast4; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getBatchDate() { return batchDate; }
    public String getBatchStatus() { return batchStatus; }
    public String getPayoutStatus() { return payoutStatus; }
    public String getFailureReason() { return failureReason; }
    public Integer getRetryCount() { return retryCount; }
    public String getTransactionReference() { return transactionReference; }
    public String getRazorpayPayoutId() { return razorpayPayoutId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ===== SETTERS =====
    public void setId(Long id) { this.id = id; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public void setBatchPayoutId(Long batchPayoutId) { this.batchPayoutId = batchPayoutId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    public void setBankAccountLast4(String bankAccountLast4) { this.bankAccountLast4 = bankAccountLast4; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setBatchDate(LocalDate batchDate) { this.batchDate = batchDate; }
    public void setBatchStatus(String batchStatus) { this.batchStatus = batchStatus; }
    public void setPayoutStatus(String payoutStatus) { this.payoutStatus = payoutStatus; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }
    public void setRazorpayPayoutId(String razorpayPayoutId) { this.razorpayPayoutId = razorpayPayoutId; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
