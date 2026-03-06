package com.farmeazy.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * SYSTEM AUDIT LOG ENTITY
 * 
 * PURPOSE: Security audit logging for sensitive operations.
 * Tracks all security-related events for compliance and forensics.
 * 
 * KEY FEATURES:
 * - Comprehensive event types (login, logout, password changes, etc.)
 * - Captures IP address and user agent for forensics
 * - Suspicious activity flagging with risk scoring
 * - Old/new value hashing for change tracking (no sensitive data)
 * 
 * SECURITY:
 * - No sensitive data stored in clear text
 * - Values are hashed for change detection
 * - IP address tracking for abuse detection
 * - Risk scoring for anomaly detection
 */
@Entity
@Table(name = "system_audit_log")
public class SystemAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "user_email", length = 255)
    private String userEmail;

    @Column(name = "action_description", nullable = false, length = 500)
    private String actionDescription;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "geo_location", length = 100)
    private String geoLocation;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "request_method", length = 10)
    private String requestMethod;

    @Column(name = "request_path", length = 500)
    private String requestPath;

    @Column(name = "response_status")
    private Integer responseStatus;

    // Hashed values for change detection - never store actual values
    @Column(name = "old_value_hash", length = 64)
    private String oldValueHash;

    @Column(name = "new_value_hash", length = 64)
    private String newValueHash;

    @Column(name = "is_suspicious")
    private Boolean isSuspicious = false;

    @Column(name = "risk_score")
    private Integer riskScore = 0;

    @Column(name = "additional_info", columnDefinition = "JSON")
    private String additionalInfo;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // No-args constructor
    public SystemAuditLog() {
    }

    // All-args constructor
    public SystemAuditLog(Long id, EventType eventType, User user, String userEmail,
                           String actionDescription, String resourceType, String resourceId,
                           String ipAddress, String userAgent, String geoLocation,
                           String sessionId, String requestMethod, String requestPath,
                           Integer responseStatus, String oldValueHash, String newValueHash,
                           Boolean isSuspicious, Integer riskScore, String additionalInfo,
                           LocalDateTime createdAt) {
        this.id = id;
        this.eventType = eventType;
        this.user = user;
        this.userEmail = userEmail;
        this.actionDescription = actionDescription;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.geoLocation = geoLocation;
        this.sessionId = sessionId;
        this.requestMethod = requestMethod;
        this.requestPath = requestPath;
        this.responseStatus = responseStatus;
        this.oldValueHash = oldValueHash;
        this.newValueHash = newValueHash;
        this.isSuspicious = isSuspicious;
        this.riskScore = riskScore;
        this.additionalInfo = additionalInfo;
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

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getActionDescription() {
        return actionDescription;
    }

    public void setActionDescription(String actionDescription) {
        this.actionDescription = actionDescription;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(String geoLocation) {
        this.geoLocation = geoLocation;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getOldValueHash() {
        return oldValueHash;
    }

    public void setOldValueHash(String oldValueHash) {
        this.oldValueHash = oldValueHash;
    }

    public String getNewValueHash() {
        return newValueHash;
    }

    public void setNewValueHash(String newValueHash) {
        this.newValueHash = newValueHash;
    }

    public Boolean getIsSuspicious() {
        return isSuspicious;
    }

    public void setIsSuspicious(Boolean isSuspicious) {
        this.isSuspicious = isSuspicious;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Event Type Enum
    public enum EventType {
        LOGIN,
        LOGOUT,
        LOGIN_FAILED,
        PASSWORD_CHANGE,
        PASSWORD_RESET,
        ACCOUNT_CREATED,
        ACCOUNT_UPDATED,
        ACCOUNT_DELETED,
        ACCOUNT_LOCKED,
        BANK_DETAILS_CHANGED,
        PAYMENT_INITIATED,
        REFUND_INITIATED,
        API_ACCESS,
        RATE_LIMIT_EXCEEDED,
        SUSPICIOUS_ACTIVITY,
        DATA_EXPORT,
        ADMIN_ACTION
    }

    /**
     * Builder pattern for creating audit logs.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final SystemAuditLog log = new SystemAuditLog();

        public Builder eventType(EventType eventType) {
            log.setEventType(eventType);
            return this;
        }

        public Builder user(User user) {
            log.setUser(user);
            if (user != null) {
                log.setUserEmail(user.getEmail());
            }
            return this;
        }

        public Builder action(String description) {
            log.setActionDescription(description);
            return this;
        }

        public Builder resource(String type, String id) {
            log.setResourceType(type);
            log.setResourceId(id);
            return this;
        }

        public Builder request(String method, String path, String ipAddress, String userAgent) {
            log.setRequestMethod(method);
            log.setRequestPath(path);
            log.setIpAddress(ipAddress);
            log.setUserAgent(userAgent);
            return this;
        }

        public Builder responseStatus(Integer status) {
            log.setResponseStatus(status);
            return this;
        }

        public Builder suspicious(boolean suspicious, int riskScore) {
            log.setIsSuspicious(suspicious);
            log.setRiskScore(riskScore);
            return this;
        }

        public Builder additionalInfo(String info) {
            log.setAdditionalInfo(info);
            return this;
        }

        public SystemAuditLog build() {
            return log;
        }
    }
}
