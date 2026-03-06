package com.farmeazy.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SERVICE REQUEST ENTITY
 * 
 * PURPOSE: Tracks user support tickets and service requests.
 * Users can raise issues about payments, orders, delivery, etc.
 * and receive support via email notifications to no-reply@farm-eazy.com.
 * 
 * KEY FEATURES:
 * - Unique request number for tracking
 * - Category-based routing
 * - Priority levels for escalation
 * - Status tracking throughout lifecycle
 * - Email notifications to support team
 */
@Entity
@Table(name = "service_request")
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_number", nullable = false, unique = true, length = 20)
    private String requestNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private RequestCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private RequestPriority priority = RequestPriority.MEDIUM;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "related_order_id")
    private Long relatedOrderId;

    @Column(name = "related_product_id")
    private Long relatedProductId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RequestStatus status = RequestStatus.OPEN;

    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolution_date")
    private LocalDateTime resolutionDate;

    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    @Column(name = "user_phone", length = 20)
    private String userPhone;

    @Column(name = "email_sent_to_support")
    private Boolean emailSentToSupport = false;

    @Column(name = "email_notification_sent")
    private Boolean emailNotificationSent = false;

    @OneToMany(mappedBy = "serviceRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ServiceRequestAttachment> attachments;

    @OneToMany(mappedBy = "serviceRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ServiceRequestComment> comments;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // No-args constructor
    public ServiceRequest() {
    }

    // All-args constructor
    public ServiceRequest(Long id, String requestNumber, User user, RequestCategory category,
                           RequestPriority priority, String subject, String description,
                           Long relatedOrderId, Long relatedProductId, RequestStatus status,
                           String assignedTo, String resolutionNotes, LocalDateTime resolutionDate,
                           String userEmail, String userPhone, Boolean emailSentToSupport,
                           Boolean emailNotificationSent, List<ServiceRequestAttachment> attachments,
                           List<ServiceRequestComment> comments, LocalDateTime createdAt,
                           LocalDateTime updatedAt) {
        this.id = id;
        this.requestNumber = requestNumber;
        this.user = user;
        this.category = category;
        this.priority = priority;
        this.subject = subject;
        this.description = description;
        this.relatedOrderId = relatedOrderId;
        this.relatedProductId = relatedProductId;
        this.status = status;
        this.assignedTo = assignedTo;
        this.resolutionNotes = resolutionNotes;
        this.resolutionDate = resolutionDate;
        this.userEmail = userEmail;
        this.userPhone = userPhone;
        this.emailSentToSupport = emailSentToSupport;
        this.emailNotificationSent = emailNotificationSent;
        this.attachments = attachments;
        this.comments = comments;
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

    public String getRequestNumber() {
        return requestNumber;
    }

    public void setRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public RequestCategory getCategory() {
        return category;
    }

    public void setCategory(RequestCategory category) {
        this.category = category;
    }

    public RequestPriority getPriority() {
        return priority;
    }

    public void setPriority(RequestPriority priority) {
        this.priority = priority;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getRelatedOrderId() {
        return relatedOrderId;
    }

    public void setRelatedOrderId(Long relatedOrderId) {
        this.relatedOrderId = relatedOrderId;
    }

    public Long getRelatedProductId() {
        return relatedProductId;
    }

    public void setRelatedProductId(Long relatedProductId) {
        this.relatedProductId = relatedProductId;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public LocalDateTime getResolutionDate() {
        return resolutionDate;
    }

    public void setResolutionDate(LocalDateTime resolutionDate) {
        this.resolutionDate = resolutionDate;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public Boolean getEmailSentToSupport() {
        return emailSentToSupport;
    }

    public void setEmailSentToSupport(Boolean emailSentToSupport) {
        this.emailSentToSupport = emailSentToSupport;
    }

    public Boolean getEmailNotificationSent() {
        return emailNotificationSent;
    }

    public void setEmailNotificationSent(Boolean emailNotificationSent) {
        this.emailNotificationSent = emailNotificationSent;
    }

    public List<ServiceRequestAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<ServiceRequestAttachment> attachments) {
        this.attachments = attachments;
    }

    public List<ServiceRequestComment> getComments() {
        return comments;
    }

    public void setComments(List<ServiceRequestComment> comments) {
        this.comments = comments;
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

    // Request Category Enum
    public enum RequestCategory {
        PAYMENT_ISSUE,
        ORDER_ISSUE,
        DELIVERY_ISSUE,
        REFUND_ISSUE,
        ACCOUNT_ISSUE,
        TECHNICAL_ISSUE,
        SELLER_COMPLAINT,
        BUYER_COMPLAINT,
        BANK_VERIFICATION,
        OTHER
    }

    // Request Priority Enum
    public enum RequestPriority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    // Request Status Enum
    public enum RequestStatus {
        OPEN,
        IN_PROGRESS,
        WAITING_FOR_USER,
        WAITING_FOR_INFO,
        RESOLVED,
        CLOSED,
        ESCALATED
    }
}
