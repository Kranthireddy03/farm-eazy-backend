package com.farmeazy.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * Entity for batch payout operations
 * Enforces ONE batch per day rule
 * Supports maker-checker approval pattern
 */
@Entity
@Table(name = "payout_batch", 
       uniqueConstraints = @UniqueConstraint(columnNames = "batch_date"),
       indexes = {
           @Index(name = "idx_payout_batch_batch_date", columnList = "batch_date"),
           @Index(name = "idx_payout_batch_status", columnList = "status"),
           @Index(name = "idx_payout_batch_created_at", columnList = "created_at")
       })
public class PayoutBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate batchDate;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "total_vendors", nullable = false)
    private Integer totalVendors = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status = BatchStatus.CREATED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedByUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ===== CONSTRUCTORS =====
    public PayoutBatch() {}

    public PayoutBatch(Long id, LocalDate batchDate, BigDecimal totalAmount, Integer totalVendors,
                      BatchStatus status, User createdByUser, User approvedByUser,
                      LocalDateTime createdAt, LocalDateTime approvedAt, LocalDateTime completedAt,
                      String failureReason, String notes, LocalDateTime updatedAt) {
        this.id = id;
        this.batchDate = batchDate;
        this.totalAmount = totalAmount;
        this.totalVendors = totalVendors;
        this.status = status;
        this.createdByUser = createdByUser;
        this.approvedByUser = approvedByUser;
        this.createdAt = createdAt;
        this.approvedAt = approvedAt;
        this.completedAt = completedAt;
        this.failureReason = failureReason;
        this.notes = notes;
        this.updatedAt = updatedAt;
    }

    // ===== GETTERS =====
    public Long getId() { return id; }
    public LocalDate getBatchDate() { return batchDate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Integer getTotalVendors() { return totalVendors; }
    public BatchStatus getStatus() { return status; }
    public User getCreatedByUser() { return createdByUser; }
    public User getApprovedByUser() { return approvedByUser; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public String getFailureReason() { return failureReason; }
    public String getNotes() { return notes; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ===== SETTERS =====
    public void setId(Long id) { this.id = id; }
    public void setBatchDate(LocalDate batchDate) { this.batchDate = batchDate; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public void setTotalVendors(Integer totalVendors) { this.totalVendors = totalVendors; }
    public void setStatus(BatchStatus status) { this.status = status; }
    public void setCreatedByUser(User createdByUser) { this.createdByUser = createdByUser; }
    public void setApprovedByUser(User approvedByUser) { this.approvedByUser = approvedByUser; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public enum BatchStatus {
        CREATED,           // Initial state
        APPROVED,          // OTP verified, approved for processing
        PROCESSING,        // Currently processing payouts
        COMPLETED,         // All payouts processed
        FAILED,            // Batch processing failed
        CANCELLED          // Manually cancelled
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
        if (totalVendors == null) totalVendors = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Validate that creator and approver are different (maker-checker)
     */
    public boolean isValidForApproval(User approver) {
        if (approver == null || createdByUser == null) return false;
        return !approver.getId().equals(createdByUser.getId());
    }

    /**
     * Check if batch can be locked for processing
     */
    public boolean canProcess() {
        return this.status == BatchStatus.APPROVED;
    }
}

