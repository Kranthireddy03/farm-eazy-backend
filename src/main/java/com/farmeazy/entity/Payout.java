package com.farmeazy.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity to track payouts from FarmEazy to sellers.
 * Implements the escrow model: Buyer pays -> FarmEazy holds -> Payout to Seller
 */
@Entity
@Table(name = "payouts")
public class Payout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_detail_id", nullable = false)
    private UserBankDetails bankDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 50)
    private ReferenceType referenceType;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(name = "gross_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "platform_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal platformFee;

    @Column(name = "net_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PayoutStatus status = PayoutStatus.PENDING;

    @Column(name = "razorpay_payout_id")
    private String razorpayPayoutId;

    @Column(name = "razorpay_fund_account_id")
    private String razorpayFundAccountId;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ReferenceType {
        PRODUCT_ORDER,
        SERVICE_BOOKING
    }

    public enum PayoutStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    public Payout() {
    }

    public Payout(User user, UserBankDetails bankDetail, ReferenceType referenceType, 
                  Long referenceId, BigDecimal grossAmount, BigDecimal platformFee) {
        this.user = user;
        this.bankDetail = bankDetail;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.grossAmount = grossAmount;
        this.platformFee = platformFee;
        this.netAmount = grossAmount.subtract(platformFee);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (netAmount == null && grossAmount != null && platformFee != null) {
            netAmount = grossAmount.subtract(platformFee);
        }
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public UserBankDetails getBankDetail() {
        return bankDetail;
    }

    public void setBankDetail(UserBankDetails bankDetail) {
        this.bankDetail = bankDetail;
    }

    public ReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(ReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
    }

    public BigDecimal getPlatformFee() {
        return platformFee;
    }

    public void setPlatformFee(BigDecimal platformFee) {
        this.platformFee = platformFee;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public PayoutStatus getStatus() {
        return status;
    }

    public void setStatus(PayoutStatus status) {
        this.status = status;
    }

    public String getRazorpayPayoutId() {
        return razorpayPayoutId;
    }

    public void setRazorpayPayoutId(String razorpayPayoutId) {
        this.razorpayPayoutId = razorpayPayoutId;
    }

    public String getRazorpayFundAccountId() {
        return razorpayFundAccountId;
    }

    public void setRazorpayFundAccountId(String razorpayFundAccountId) {
        this.razorpayFundAccountId = razorpayFundAccountId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
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
}
