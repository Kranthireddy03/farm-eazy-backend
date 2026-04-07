package com.farmeazy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Notification template for payout system
 * Stores HTML and text templates for different notification types
 */
@Entity
@Table(name = "notification_template",
       uniqueConstraints = @UniqueConstraint(columnNames = "template_name"),
       indexes = {
           @Index(name = "idx_notification_type", columnList = "notification_type")
       })
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String templateName;

    @Column(nullable = false, length = 50)
    private String notificationType;

    @Column(nullable = false, length = 255)
    private String subjectTemplate;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String htmlTemplate;

    @Column(columnDefinition = "LONGTEXT")
    private String textTemplate;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ===== CONSTRUCTORS =====
    public NotificationTemplate() {}

    public NotificationTemplate(Long id, String templateName, String notificationType, String subjectTemplate,
                               String htmlTemplate, String textTemplate, Boolean isActive,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.templateName = templateName;
        this.notificationType = notificationType;
        this.subjectTemplate = subjectTemplate;
        this.htmlTemplate = htmlTemplate;
        this.textTemplate = textTemplate;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ===== GETTERS =====
    public Long getId() { return id; }
    public String getTemplateName() { return templateName; }
    public String getNotificationType() { return notificationType; }
    public String getSubjectTemplate() { return subjectTemplate; }
    public String getHtmlTemplate() { return htmlTemplate; }
    public String getTextTemplate() { return textTemplate; }
    public Boolean getIsActive() { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ===== SETTERS =====
    public void setId(Long id) { this.id = id; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }
    public void setSubjectTemplate(String subjectTemplate) { this.subjectTemplate = subjectTemplate; }
    public void setHtmlTemplate(String htmlTemplate) { this.htmlTemplate = htmlTemplate; }
    public void setTextTemplate(String textTemplate) { this.textTemplate = textTemplate; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
