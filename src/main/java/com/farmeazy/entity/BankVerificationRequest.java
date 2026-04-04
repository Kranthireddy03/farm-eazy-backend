package com.farmeazy.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * BANK VERIFICATION REQUEST ENTITY
 * 
 * PURPOSE: Tracks bank account verification requests using 1 rupee transfers.
 * Implements professional handling with rate limiting to prevent abuse.
 * 
 * KEY FEATURES:
 * - Unique verification number for tracking
 * - Masked bank details for security (no sensitive data in logs)
 * - Transfer status tracking
 * - Rate limiting fields to prevent abuse
 * - User notification tracking
 * 
 * SECURITY:
 * - Account numbers are stored as hashes
 * - Only masked versions shown in UI/logs
 * - Rate limits prevent excessive verification attempts
 */
@Entity
@Table(name = "bank_verification_request")
public class BankVerificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "verification_number", nullable = false, unique = true, length = 20)
    private String verificationNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_type", nullable = false)
    private VerificationType verificationType;

    // Masked bank details - NEVER store full account numbers in logs
    @Column(name = "account_holder_name", length = 100)
    private String accountHolderName;

    @Column(name = "account_number_masked", length = 20)
    private String accountNumberMasked;

    @Column(name = "account_number_hash", length = 64)
    private String accountNumberHash;

    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "branch_name", length = 100)
    private String branchName;

    @Column(name = "upi_id_masked", length = 50)
    private String upiIdMasked;

    @Column(name = "upi_id_hash", length = 64)
    private String upiIdHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private VerificationStatus status = VerificationStatus.INITIATED;

    @Column(name = "transfer_amount", precision = 5, scale = 2)
    private BigDecimal transferAmount = new BigDecimal("1.00");

    @Column(name = "transfer_reference_id", length = 100)
    private String transferReferenceId;

    @Column(name = "razorpay_contact_id", length = 100)
    private String razorpayContactId;

    @Column(name = "razorpay_fund_account_id", length = 100)
    private String razorpayFundAccountId;

    @Column(name = "transfer_gateway", length = 50)
    private String transferGateway;

    @Column(name = "transfer_status", length = 50)
    private String transferStatus;

    @Column(name = "transfer_error_message", length = 500)
    private String transferErrorMessage;

    @Column(name = "transfer_attempted_at")
    private LocalDateTime transferAttemptedAt;

    @Column(name = "transfer_completed_at")
    private LocalDateTime transferCompletedAt;

    // Rate limiting fields
    @Column(name = "verification_attempts_today")
    private Integer verificationAttemptsToday = 1;

    @Column(name = "last_verification_date")
    private LocalDate lastVerificationDate;

    @Column(name = "total_verification_attempts")
    private Integer totalVerificationAttempts = 1;

    @Column(name = "daily_limit_reached")
    private Boolean dailyLimitReached = false;

    // User notification
    @Column(name = "user_notified")
    private Boolean userNotified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type")
    private NotificationType notificationType;

    @Column(name = "notification_sent_at")
    private LocalDateTime notificationSentAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public BankVerificationRequest() {
    }

    public BankVerificationRequest(Long id, String verificationNumber, User user, VerificationType verificationType,
            String accountHolderName, String accountNumberMasked, String accountNumberHash, String ifscCode,
            String bankName, String branchName, String upiIdMasked, String upiIdHash, VerificationStatus status,
            BigDecimal transferAmount, String transferReferenceId, String razorpayContactId, String razorpayFundAccountId,
            String transferGateway, String transferStatus,
            String transferErrorMessage, LocalDateTime transferAttemptedAt, LocalDateTime transferCompletedAt,
            Integer verificationAttemptsToday, LocalDate lastVerificationDate, Integer totalVerificationAttempts,
            Boolean dailyLimitReached, Boolean userNotified, NotificationType notificationType,
            LocalDateTime notificationSentAt, LocalDateTime verifiedAt, LocalDateTime expiresAt,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.verificationNumber = verificationNumber;
        this.user = user;
        this.verificationType = verificationType;
        this.accountHolderName = accountHolderName;
        this.accountNumberMasked = accountNumberMasked;
        this.accountNumberHash = accountNumberHash;
        this.ifscCode = ifscCode;
        this.bankName = bankName;
        this.branchName = branchName;
        this.upiIdMasked = upiIdMasked;
        this.upiIdHash = upiIdHash;
        this.status = status;
        this.transferAmount = transferAmount;
        this.transferReferenceId = transferReferenceId;
        this.razorpayContactId = razorpayContactId;
        this.razorpayFundAccountId = razorpayFundAccountId;
        this.transferGateway = transferGateway;
        this.transferStatus = transferStatus;
        this.transferErrorMessage = transferErrorMessage;
        this.transferAttemptedAt = transferAttemptedAt;
        this.transferCompletedAt = transferCompletedAt;
        this.verificationAttemptsToday = verificationAttemptsToday;
        this.lastVerificationDate = lastVerificationDate;
        this.totalVerificationAttempts = totalVerificationAttempts;
        this.dailyLimitReached = dailyLimitReached;
        this.userNotified = userNotified;
        this.notificationType = notificationType;
        this.notificationSentAt = notificationSentAt;
        this.verifiedAt = verifiedAt;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        lastVerificationDate = LocalDate.now();
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

    public String getVerificationNumber() {
        return verificationNumber;
    }

    public void setVerificationNumber(String verificationNumber) {
        this.verificationNumber = verificationNumber;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public VerificationType getVerificationType() {
        return verificationType;
    }

    public void setVerificationType(VerificationType verificationType) {
        this.verificationType = verificationType;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getAccountNumberMasked() {
        return accountNumberMasked;
    }

    public void setAccountNumberMasked(String accountNumberMasked) {
        this.accountNumberMasked = accountNumberMasked;
    }

    public String getAccountNumberHash() {
        return accountNumberHash;
    }

    public void setAccountNumberHash(String accountNumberHash) {
        this.accountNumberHash = accountNumberHash;
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

    public String getUpiIdMasked() {
        return upiIdMasked;
    }

    public void setUpiIdMasked(String upiIdMasked) {
        this.upiIdMasked = upiIdMasked;
    }

    public String getUpiIdHash() {
        return upiIdHash;
    }

    public void setUpiIdHash(String upiIdHash) {
        this.upiIdHash = upiIdHash;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public void setStatus(VerificationStatus status) {
        this.status = status;
    }

    public BigDecimal getTransferAmount() {
        return transferAmount;
    }

    public void setTransferAmount(BigDecimal transferAmount) {
        this.transferAmount = transferAmount;
    }

    public String getTransferReferenceId() {
        return transferReferenceId;
    }

    public void setTransferReferenceId(String transferReferenceId) {
        this.transferReferenceId = transferReferenceId;
    }

    public String getRazorpayContactId() {
        return razorpayContactId;
    }

    public void setRazorpayContactId(String razorpayContactId) {
        this.razorpayContactId = razorpayContactId;
    }

    public String getRazorpayFundAccountId() {
        return razorpayFundAccountId;
    }

    public void setRazorpayFundAccountId(String razorpayFundAccountId) {
        this.razorpayFundAccountId = razorpayFundAccountId;
    }

    public String getTransferGateway() {
        return transferGateway;
    }

    public void setTransferGateway(String transferGateway) {
        this.transferGateway = transferGateway;
    }

    public String getTransferStatus() {
        return transferStatus;
    }

    public void setTransferStatus(String transferStatus) {
        this.transferStatus = transferStatus;
    }

    public String getTransferErrorMessage() {
        return transferErrorMessage;
    }

    public void setTransferErrorMessage(String transferErrorMessage) {
        this.transferErrorMessage = transferErrorMessage;
    }

    public LocalDateTime getTransferAttemptedAt() {
        return transferAttemptedAt;
    }

    public void setTransferAttemptedAt(LocalDateTime transferAttemptedAt) {
        this.transferAttemptedAt = transferAttemptedAt;
    }

    public LocalDateTime getTransferCompletedAt() {
        return transferCompletedAt;
    }

    public void setTransferCompletedAt(LocalDateTime transferCompletedAt) {
        this.transferCompletedAt = transferCompletedAt;
    }

    public Integer getVerificationAttemptsToday() {
        return verificationAttemptsToday;
    }

    public void setVerificationAttemptsToday(Integer verificationAttemptsToday) {
        this.verificationAttemptsToday = verificationAttemptsToday;
    }

    public LocalDate getLastVerificationDate() {
        return lastVerificationDate;
    }

    public void setLastVerificationDate(LocalDate lastVerificationDate) {
        this.lastVerificationDate = lastVerificationDate;
    }

    public Integer getTotalVerificationAttempts() {
        return totalVerificationAttempts;
    }

    public void setTotalVerificationAttempts(Integer totalVerificationAttempts) {
        this.totalVerificationAttempts = totalVerificationAttempts;
    }

    public Boolean getDailyLimitReached() {
        return dailyLimitReached;
    }

    public void setDailyLimitReached(Boolean dailyLimitReached) {
        this.dailyLimitReached = dailyLimitReached;
    }

    public Boolean getUserNotified() {
        return userNotified;
    }

    public void setUserNotified(Boolean userNotified) {
        this.userNotified = userNotified;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public LocalDateTime getNotificationSentAt() {
        return notificationSentAt;
    }

    public void setNotificationSentAt(LocalDateTime notificationSentAt) {
        this.notificationSentAt = notificationSentAt;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
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

    // Verification Type Enum
    public enum VerificationType {
        BANK_ACCOUNT,
        UPI
    }

    // Verification Status Enum
    public enum VerificationStatus {
        INITIATED,
        TRANSFER_PENDING,
        TRANSFER_SUCCESS,
        TRANSFER_FAILED,
        VERIFIED,
        REJECTED,
        EXPIRED,
        CANCELLED
    }

    // Notification Type Enum
    public enum NotificationType {
        EMAIL,
        SMS,
        BOTH
    }
}
