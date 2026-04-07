package com.farmeazy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Immutable audit log for payout operations
 * Append-only table (no updates/deletes allowed)
 * Retention: Keep for 7 years minimum
 */
@Entity
@Table(name = "payout_audit",
       indexes = {
           @Index(name = "idx_audit_batch", columnList = "batch_id"),
           @Index(name = "idx_audit_batch_payout", columnList = "batch_payout_id"),
           @Index(name = "idx_audit_action", columnList = "action"),
           @Index(name = "idx_audit_timestamp", columnList = "timestamp")
       })
public class PayoutAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private PayoutBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_payout_id")
    private BatchPayout batchPayout;

    @Column(nullable = false, length = 50)
    private String action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_by_user_id")
    private User actionByUser;

    @Column(name = "action_detail", length = 500)
    private String actionDetail;

    @Column(name = "previous_status", length = 50)
    private String previousStatus;

    @Column(name = "new_status", length = 50)
    private String newStatus;

    @Column(name = "affected_field", length = 100)
    private String affectedField;

    @Column(name = "affected_value_before", columnDefinition = "TEXT")
    private String affectedValueBefore;

    @Column(name = "affected_value_after", columnDefinition = "TEXT")
    private String affectedValueAfter;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    // ===== CONSTRUCTORS =====
    public PayoutAudit() {}

    public PayoutAudit(Long id, PayoutBatch batch, BatchPayout batchPayout, String action,
                      User actionByUser, String actionDetail, String previousStatus, String newStatus,
                      String affectedField, String affectedValueBefore, String affectedValueAfter,
                      String ipAddress, String userAgent, LocalDateTime timestamp) {
        this.id = id;
        this.batch = batch;
        this.batchPayout = batchPayout;
        this.action = action;
        this.actionByUser = actionByUser;
        this.actionDetail = actionDetail;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.affectedField = affectedField;
        this.affectedValueBefore = affectedValueBefore;
        this.affectedValueAfter = affectedValueAfter;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.timestamp = timestamp;
    }

    // ===== GETTERS =====
    public Long getId() { return id; }
    public PayoutBatch getBatch() { return batch; }
    public BatchPayout getBatchPayout() { return batchPayout; }
    public String getAction() { return action; }
    public User getActionByUser() { return actionByUser; }
    public String getActionDetail() { return actionDetail; }
    public String getPreviousStatus() { return previousStatus; }
    public String getNewStatus() { return newStatus; }
    public String getAffectedField() { return affectedField; }
    public String getAffectedValueBefore() { return affectedValueBefore; }
    public String getAffectedValueAfter() { return affectedValueAfter; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public LocalDateTime getTimestamp() { return timestamp; }

    // ===== SETTERS =====
    public void setId(Long id) { this.id = id; }
    public void setBatch(PayoutBatch batch) { this.batch = batch; }
    public void setBatchPayout(BatchPayout batchPayout) { this.batchPayout = batchPayout; }
    public void setAction(String action) { this.action = action; }
    public void setActionByUser(User actionByUser) { this.actionByUser = actionByUser; }
    public void setActionDetail(String actionDetail) { this.actionDetail = actionDetail; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
    public void setAffectedField(String affectedField) { this.affectedField = affectedField; }
    public void setAffectedValueBefore(String affectedValueBefore) { this.affectedValueBefore = affectedValueBefore; }
    public void setAffectedValueAfter(String affectedValueAfter) { this.affectedValueAfter = affectedValueAfter; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
