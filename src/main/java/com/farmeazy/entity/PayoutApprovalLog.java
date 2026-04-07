package com.farmeazy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Records OTP-based approvals for maker-checker pattern
 * Each approval requires valid OTP verification
 */
@Entity
@Table(name = "payout_approval_log",
       indexes = {
           @Index(name = "idx_approval_batch", columnList = "batch_id"),
           @Index(name = "idx_approval_user", columnList = "approved_by_user_id")
       })
public class PayoutApprovalLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private PayoutBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id", nullable = false)
    private User approvedByUser;

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Column(name = "otp_verified_at", nullable = false)
    private LocalDateTime otpVerifiedAt;

    @Column(name = "approval_ip_address", length = 45)
    private String approvalIpAddress;

    @Column(name = "approval_device_info", length = 500)
    private String approvalDeviceInfo;

    @Column(name = "approved_at", nullable = false, updatable = false)
    private LocalDateTime approvedAt;

    // ===== CONSTRUCTORS =====
    public PayoutApprovalLog() {}

    public PayoutApprovalLog(Long id, PayoutBatch batch, User approvedByUser, String otpCode,
                            LocalDateTime otpVerifiedAt, String approvalIpAddress, 
                            String approvalDeviceInfo, LocalDateTime approvedAt) {
        this.id = id;
        this.batch = batch;
        this.approvedByUser = approvedByUser;
        this.otpCode = otpCode;
        this.otpVerifiedAt = otpVerifiedAt;
        this.approvalIpAddress = approvalIpAddress;
        this.approvalDeviceInfo = approvalDeviceInfo;
        this.approvedAt = approvedAt;
    }

    // ===== GETTERS =====
    public Long getId() { return id; }
    public PayoutBatch getBatch() { return batch; }
    public User getApprovedByUser() { return approvedByUser; }
    public String getOtpCode() { return otpCode; }
    public LocalDateTime getOtpVerifiedAt() { return otpVerifiedAt; }
    public String getApprovalIpAddress() { return approvalIpAddress; }
    public String getApprovalDeviceInfo() { return approvalDeviceInfo; }
    public LocalDateTime getApprovedAt() { return approvedAt; }

    // ===== SETTERS =====
    public void setId(Long id) { this.id = id; }
    public void setBatch(PayoutBatch batch) { this.batch = batch; }
    public void setApprovedByUser(User approvedByUser) { this.approvedByUser = approvedByUser; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
    public void setOtpVerifiedAt(LocalDateTime otpVerifiedAt) { this.otpVerifiedAt = otpVerifiedAt; }
    public void setApprovalIpAddress(String approvalIpAddress) { this.approvalIpAddress = approvalIpAddress; }
    public void setApprovalDeviceInfo(String approvalDeviceInfo) { this.approvalDeviceInfo = approvalDeviceInfo; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    @PrePersist
    protected void onCreate() {
        approvedAt = LocalDateTime.now();
    }
}
