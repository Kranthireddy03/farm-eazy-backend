package com.farmeazy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * USER REFUND DETAILS ENTITY
 * 
 * Purpose: Store buyer's bank/UPI details for receiving refunds.
 * Separate from UserBankDetails (which is for sellers receiving payouts).
 * 
 * Features:
 * - One record per user (reusable for all refunds)
 * - Support both bank account and UPI refunds
 * - Verification status tracking
 * - Preferred refund method selection
 * 
 * @author FarmEazy Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "user_refund_details")
public class UserRefundDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "account_holder_name", length = 100, nullable = false)
    private String accountHolderName;

    @Column(name = "account_number", length = 30)
    private String accountNumber;

    @Column(name = "confirm_account_number", length = 30)
    private String confirmAccountNumber;

    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "branch_name", length = 100)
    private String branchName;

    @Column(name = "upi_id", length = 100)
    private String upiId;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_method", length = 10)
    private RefundMethod preferredMethod = RefundMethod.UPI;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "verification_date")
    private LocalDateTime verificationDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public UserRefundDetails() {
    }

    public UserRefundDetails(User user, String accountHolderName) {
        this.user = user;
        this.accountHolderName = accountHolderName;
    }

    // Lifecycle callbacks
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Enums
    public enum RefundMethod {
        BANK,
        UPI
    }

    // Utility methods
    public boolean hasBankDetails() {
        return accountNumber != null && !accountNumber.isEmpty() 
            && ifscCode != null && !ifscCode.isEmpty();
    }

    public boolean hasUpiDetails() {
        return upiId != null && !upiId.isEmpty();
    }

    public boolean hasValidRefundDetails() {
        if (preferredMethod == RefundMethod.BANK) {
            return hasBankDetails();
        } else {
            return hasUpiDetails();
        }
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

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getConfirmAccountNumber() {
        return confirmAccountNumber;
    }

    public void setConfirmAccountNumber(String confirmAccountNumber) {
        this.confirmAccountNumber = confirmAccountNumber;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    public RefundMethod getPreferredMethod() {
        return preferredMethod;
    }

    public void setPreferredMethod(RefundMethod preferredMethod) {
        this.preferredMethod = preferredMethod;
    }

    public Boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
    }

    public LocalDateTime getVerificationDate() {
        return verificationDate;
    }

    public void setVerificationDate(LocalDateTime verificationDate) {
        this.verificationDate = verificationDate;
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
