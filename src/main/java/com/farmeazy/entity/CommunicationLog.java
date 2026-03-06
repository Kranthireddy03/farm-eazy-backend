package com.farmeazy.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * COMMUNICATION LOG ENTITY
 * 
 * PURPOSE: Tracks all email/SMS communications sent to users.
 * Provides audit trail for all notifications with delivery status.
 * 
 * KEY FEATURES:
 * - Tracks communication type (email, SMS, push)
 * - Records purpose and template used
 * - Stores delivery status and provider responses
 * - Supports retry tracking for failed deliveries
 * - Links to reference entities (orders, payments, etc.)
 * 
 * SECURITY:
 * - Only stores content summary, not full content
 * - Full content hash for verification
 * - No sensitive data in logs
 */
@Entity
@Table(name = "communication_log")
public class CommunicationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "communication_type", nullable = false)
    private CommunicationType communicationType;

    @Column(name = "template_name", length = 100)
    private String templateName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id")
    private User recipientUser;

    @Column(name = "recipient_email", length = 255)
    private String recipientEmail;

    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @Column(name = "subject", length = 500)
    private String subject;

    // Only summary, no sensitive content
    @Column(name = "content_summary", length = 1000)
    private String contentSummary;

    @Column(name = "full_content_hash", length = 64)
    private String fullContentHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false)
    private CommunicationPurpose purpose;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CommunicationStatus status = CommunicationStatus.QUEUED;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "provider_message_id", length = 100)
    private String providerMessageId;

    @Column(name = "provider_response", length = 500)
    private String providerResponse;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "clicked_at")
    private LocalDateTime clickedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // No-args constructor
    public CommunicationLog() {
    }

    // All-args constructor
    public CommunicationLog(Long id, CommunicationType communicationType, String templateName,
                             User recipientUser, String recipientEmail, String recipientPhone,
                             String subject, String contentSummary, String fullContentHash,
                             CommunicationPurpose purpose, String referenceType, Long referenceId,
                             CommunicationStatus status, String provider, String providerMessageId,
                             String providerResponse, String errorMessage, Integer retryCount,
                             LocalDateTime scheduledAt, LocalDateTime sentAt, LocalDateTime deliveredAt,
                             LocalDateTime openedAt, LocalDateTime clickedAt, LocalDateTime createdAt) {
        this.id = id;
        this.communicationType = communicationType;
        this.templateName = templateName;
        this.recipientUser = recipientUser;
        this.recipientEmail = recipientEmail;
        this.recipientPhone = recipientPhone;
        this.subject = subject;
        this.contentSummary = contentSummary;
        this.fullContentHash = fullContentHash;
        this.purpose = purpose;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.status = status;
        this.provider = provider;
        this.providerMessageId = providerMessageId;
        this.providerResponse = providerResponse;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.scheduledAt = scheduledAt;
        this.sentAt = sentAt;
        this.deliveredAt = deliveredAt;
        this.openedAt = openedAt;
        this.clickedAt = clickedAt;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CommunicationType getCommunicationType() {
        return communicationType;
    }

    public void setCommunicationType(CommunicationType communicationType) {
        this.communicationType = communicationType;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public User getRecipientUser() {
        return recipientUser;
    }

    public void setRecipientUser(User recipientUser) {
        this.recipientUser = recipientUser;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContentSummary() {
        return contentSummary;
    }

    public void setContentSummary(String contentSummary) {
        this.contentSummary = contentSummary;
    }

    public String getFullContentHash() {
        return fullContentHash;
    }

    public void setFullContentHash(String fullContentHash) {
        this.fullContentHash = fullContentHash;
    }

    public CommunicationPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(CommunicationPurpose purpose) {
        this.purpose = purpose;
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

    public CommunicationStatus getStatus() {
        return status;
    }

    public void setStatus(CommunicationStatus status) {
        this.status = status;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public String getProviderResponse() {
        return providerResponse;
    }

    public void setProviderResponse(String providerResponse) {
        this.providerResponse = providerResponse;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(LocalDateTime openedAt) {
        this.openedAt = openedAt;
    }

    public LocalDateTime getClickedAt() {
        return clickedAt;
    }

    public void setClickedAt(LocalDateTime clickedAt) {
        this.clickedAt = clickedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Communication Type Enum
    public enum CommunicationType {
        EMAIL,
        SMS,
        PUSH_NOTIFICATION,
        IN_APP
    }

    // Communication Purpose Enum
    public enum CommunicationPurpose {
        WELCOME,
        PASSWORD_RESET,
        OTP,
        ORDER_CONFIRMATION,
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        REFUND_INITIATED,
        REFUND_COMPLETED,
        BANK_VERIFICATION,
        SERVICE_REQUEST,
        PAYOUT,
        DELIVERY_UPDATE,
        IRRIGATION_REMINDER,
        MARKETING,
        SYSTEM_ALERT,
        OTHER
    }

    // Communication Status Enum
    public enum CommunicationStatus {
        QUEUED,
        SENT,
        DELIVERED,
        FAILED,
        BOUNCED,
        OPENED,
        CLICKED
    }
}
