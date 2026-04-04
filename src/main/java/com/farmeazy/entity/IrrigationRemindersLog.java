package com.farmeazy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * IrrigationRemindersLog Entity
 * Audit trail for all reminder notifications sent to farmers
 */
@Entity
@Table(name = "irrigation_reminders_log")
public class IrrigationRemindersLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long farmId;

    @Column
    private Long irrigationScheduleId;

    @Column(nullable = false)
    private String reminderType; // SMS, EMAIL, PUSH, IN_APP

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private String recipientAddress;

    @Column(nullable = false)
    private String status; // SENT, FAILED, BOUNCED, DELIVERED

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column
    private Integer retryCount = 0;

    @Column
    private Integer maxRetries = 3;

    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @Column
    private LocalDateTime deliveredAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public IrrigationRemindersLog() {}

    public IrrigationRemindersLog(Long userId, Long farmId, String reminderType,
                                  String message, String recipientAddress) {
        this.userId = userId;
        this.farmId = farmId;
        this.reminderType = reminderType;
        this.message = message;
        this.recipientAddress = recipientAddress;
        this.status = "SENT";
        this.retryCount = 0;
        this.maxRetries = 3;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }

    public Long getIrrigationScheduleId() { return irrigationScheduleId; }
    public void setIrrigationScheduleId(Long irrigationScheduleId) { this.irrigationScheduleId = irrigationScheduleId; }

    public String getReminderType() { return reminderType; }
    public void setReminderType(String reminderType) { this.reminderType = reminderType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getRecipientAddress() { return recipientAddress; }
    public void setRecipientAddress(String recipientAddress) { this.recipientAddress = recipientAddress; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        sentAt = LocalDateTime.now();
    }
}
