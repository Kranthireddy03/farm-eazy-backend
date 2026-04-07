package com.farmeazy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Configuration management for payout system
 * Allows adjustment of rules without code changes
 */
@Entity
@Table(name = "payout_config",
       uniqueConstraints = @UniqueConstraint(columnNames = "config_key"))
public class PayoutConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String configKey;

    @Column(nullable = false, length = 500)
    private String configValue;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private User updatedByUser;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ===== CONSTRUCTORS =====
    public PayoutConfig() {}

    public PayoutConfig(Long id, String configKey, String configValue, String description,
                       Boolean isActive, User updatedByUser, LocalDateTime updatedAt) {
        this.id = id;
        this.configKey = configKey;
        this.configValue = configValue;
        this.description = description;
        this.isActive = isActive;
        this.updatedByUser = updatedByUser;
        this.updatedAt = updatedAt;
    }

    // ===== GETTERS =====
    public Long getId() { return id; }
    public String getConfigKey() { return configKey; }
    public String getConfigValue() { return configValue; }
    public String getDescription() { return description; }
    public Boolean getIsActive() { return isActive; }
    public User getUpdatedByUser() { return updatedByUser; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ===== SETTERS =====
    public void setId(Long id) { this.id = id; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public void setDescription(String description) { this.description = description; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public void setUpdatedByUser(User updatedByUser) { this.updatedByUser = updatedByUser; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods for common config keys
    public static String KEY_DAILY_BATCH_LIMIT = "DAILY_BATCH_LIMIT";
    public static String KEY_MAX_RETRY_ATTEMPTS = "MAX_RETRY_ATTEMPTS";
    public static String KEY_PAYOUT_MIN_AMOUNT = "PAYOUT_MIN_AMOUNT";
    public static String KEY_PAYOUT_MAX_AMOUNT = "PAYOUT_MAX_AMOUNT";
    public static String KEY_DAILY_MAX_TOTAL = "DAILY_MAX_TOTAL";
    public static String KEY_OTP_EXPIRY_MINUTES = "OTP_EXPIRY_MINUTES";
    public static String KEY_APPROVAL_REQUIRED = "APPROVAL_REQUIRED";
}
