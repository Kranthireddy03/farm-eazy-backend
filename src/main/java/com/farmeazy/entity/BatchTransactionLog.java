package com.farmeazy.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * BATCH TRANSACTION LOG ENTITY
 * 
 * PURPOSE: Maintains a separate audit trail for all batch-related financial transactions.
 * This ensures all batch processing activities are properly stamped and traceable.
 * 
 * KEY FEATURES:
 * - Links to parent batch job
 * - Tracks transaction type (reversal, payout, refund, verification)
 * - Stores masked bank/UPI details for security
 * - Records payment gateway responses
 * - Maintains complete audit trail
 */
@Entity
@Table(name = "batch_transaction_log")
public class BatchTransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_job_id", nullable = false)
    private BatchJobExecution batchJob;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Column(name = "reference_type", nullable = false, length = 50)
    private String referenceType;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @Column(name = "payment_gateway", length = 50)
    private String paymentGateway;

    @Column(name = "gateway_transaction_id", length = 100)
    private String gatewayTransactionId;

    @Column(name = "gateway_response_code", length = 20)
    private String gatewayResponseCode;

    @Column(name = "gateway_response_message", length = 500)
    private String gatewayResponseMessage;

    // Masked for security - no sensitive data in logs
    @Column(name = "bank_account_masked", length = 20)
    private String bankAccountMasked;

    @Column(name = "upi_id_masked", length = 50)
    private String upiIdMasked;

    @Column(name = "attempt_number")
    private Integer attemptNumber = 1;

    @Column(name = "previous_status", length = 30)
    private String previousStatus;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // No-args constructor
    public BatchTransactionLog() {
    }

    // All-args constructor
    public BatchTransactionLog(Long id, BatchJobExecution batchJob, TransactionType transactionType,
                                String referenceType, Long referenceId, User user, BigDecimal amount,
                                String currency, TransactionStatus status, String paymentGateway,
                                String gatewayTransactionId, String gatewayResponseCode,
                                String gatewayResponseMessage, String bankAccountMasked,
                                String upiIdMasked, Integer attemptNumber, String previousStatus,
                                String notes, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.batchJob = batchJob;
        this.transactionType = transactionType;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.user = user;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.paymentGateway = paymentGateway;
        this.gatewayTransactionId = gatewayTransactionId;
        this.gatewayResponseCode = gatewayResponseCode;
        this.gatewayResponseMessage = gatewayResponseMessage;
        this.bankAccountMasked = bankAccountMasked;
        this.upiIdMasked = upiIdMasked;
        this.attemptNumber = attemptNumber;
        this.previousStatus = previousStatus;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BatchJobExecution getBatchJob() {
        return batchJob;
    }

    public void setBatchJob(BatchJobExecution batchJob) {
        this.batchJob = batchJob;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getPaymentGateway() {
        return paymentGateway;
    }

    public void setPaymentGateway(String paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public String getGatewayResponseCode() {
        return gatewayResponseCode;
    }

    public void setGatewayResponseCode(String gatewayResponseCode) {
        this.gatewayResponseCode = gatewayResponseCode;
    }

    public String getGatewayResponseMessage() {
        return gatewayResponseMessage;
    }

    public void setGatewayResponseMessage(String gatewayResponseMessage) {
        this.gatewayResponseMessage = gatewayResponseMessage;
    }

    public String getBankAccountMasked() {
        return bankAccountMasked;
    }

    public void setBankAccountMasked(String bankAccountMasked) {
        this.bankAccountMasked = bankAccountMasked;
    }

    public String getUpiIdMasked() {
        return upiIdMasked;
    }

    public void setUpiIdMasked(String upiIdMasked) {
        this.upiIdMasked = upiIdMasked;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Transaction Type Enum
    public enum TransactionType {
        REVERSAL,
        PAYOUT,
        REFUND,
        VERIFICATION_CREDIT,
        VERIFICATION_DEBIT
    }

    // Transaction Status Enum
    public enum TransactionStatus {
        INITIATED,
        PROCESSING,
        SUCCESS,
        FAILED,
        REVERSED
    }
}
