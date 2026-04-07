package com.farmeazy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Fraud detection log for security monitoring
 * Tracks suspicious patterns and risk assessment
 */
@Entity
@Table(name = "fraud_detection_log",
       indexes = {
           @Index(name = "idx_user_id", columnList = "user_id"),
           @Index(name = "idx_risk_level", columnList = "risk_level")
       })
public class FraudDetectionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column
    private Long batchId;

    @Column(length = 50)
    private String fraudType;  // UNUSUAL_AMOUNT, UNUSUAL_FREQUENCY, DUPLICATE_REQUEST, SUSPICIOUS_IP, VELOCITY_CHECK

    @Column
    private Integer riskScore = 0;  // 0-100

    @Column(length = 20)
    private String riskLevel;  // LOW, MEDIUM, HIGH

    @Column(columnDefinition = "JSON")
    private String details;

    @Column(length = 100)
    private String actionTaken;  // APPROVED, FLAGGED, BLOCKED, MANUAL_REVIEW

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ===== CONSTRUCTORS =====
    public FraudDetectionLog() {}

    public FraudDetectionLog(Long id, Long userId, Long batchId, String fraudType, Integer riskScore,
                            String riskLevel, String details, String actionTaken, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.batchId = batchId;
        this.fraudType = fraudType;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.details = details;
        this.actionTaken = actionTaken;
        this.createdAt = createdAt;
    }

    // ===== GETTERS =====
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getBatchId() { return batchId; }
    public String getFraudType() { return fraudType; }
    public Integer getRiskScore() { return riskScore; }
    public String getRiskLevel() { return riskLevel; }
    public String getDetails() { return details; }
    public String getActionTaken() { return actionTaken; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ===== SETTERS =====
    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public void setFraudType(String fraudType) { this.fraudType = fraudType; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public void setDetails(String details) { this.details = details; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
