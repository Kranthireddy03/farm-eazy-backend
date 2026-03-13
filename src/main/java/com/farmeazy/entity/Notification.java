package com.farmeazy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * NOTIFICATION ENTITY
 * 
 * PURPOSE: Stores in-app notifications for users.
 * Supports both user-specific and broadcast notifications.
 * 
 * STORAGE OPTIMIZATION:
 * - Short VARCHAR limits on title/message
 * - Auto-cleanup of old read notifications (30 days)
 * - Indexed on userId and isRead for fast queries
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notif_user_read", columnList = "user_id, is_read"),
    @Index(name = "idx_notif_created", columnList = "created_at")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User receiving the notification.
     * NULL = broadcast to all users
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Notification type for icon/styling
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private NotificationType type;

    /**
     * Short title (max 100 chars)
     */
    @Column(name = "title", length = 100, nullable = false)
    private String title;

    /**
     * Notification message (max 500 chars to save space)
     */
    @Column(name = "message", length = 500, nullable = false)
    private String message;

    /**
     * Optional action URL when notification is clicked
     */
    @Column(name = "action_url", length = 200)
    private String actionUrl;

    /**
     * Read status
     */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    /**
     * For broadcast notifications - track which users dismissed it
     * Stored as comma-separated user IDs (efficient for small broadcasts)
     */
    @Column(name = "dismissed_by", length = 2000)
    private String dismissedBy;

    /**
     * Is this a broadcast notification (sent to all users)?
     */
    @Column(name = "is_broadcast", nullable = false)
    private Boolean isBroadcast = false;

    /**
     * Priority: LOW, NORMAL, HIGH, URGENT
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 10)
    private NotificationPriority priority = NotificationPriority.NORMAL;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expiresAt == null) {
            // Default: expire after 30 days
            expiresAt = createdAt.plusDays(30);
        }
    }

    // Enums
    public enum NotificationType {
        ORDER,          // Order related
        PAYMENT,        // Payment success/failure
        FARM,           // Farm operations
        IRRIGATION,     // Irrigation reminders
        PRODUCT,        // Marketplace products
        ACCOUNT,        // Account changes
        SYSTEM,         // System announcements
        PROMO           // Promotional offers
    }

    public enum NotificationPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }

    // Constructors
    public Notification() {}

    public Notification(User user, NotificationType type, String title, String message) {
        this.user = user;
        this.type = type;
        this.title = title;
        this.message = message;
        this.isBroadcast = (user == null);
    }

    // Static factory for broadcast
    public static Notification broadcast(NotificationType type, String title, String message) {
        Notification n = new Notification();
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setIsBroadcast(true);
        return n;
    }

    // Check if dismissed by a specific user
    public boolean isDismissedByUser(Long userId) {
        if (dismissedBy == null || dismissedBy.isEmpty()) return false;
        return dismissedBy.contains("," + userId + ",") || 
               dismissedBy.startsWith(userId + ",") ||
               dismissedBy.endsWith("," + userId) ||
               dismissedBy.equals(String.valueOf(userId));
    }

    // Add user to dismissed list
    public void dismissForUser(Long userId) {
        if (dismissedBy == null || dismissedBy.isEmpty()) {
            dismissedBy = String.valueOf(userId);
        } else if (!isDismissedByUser(userId)) {
            dismissedBy += "," + userId;
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public String getDismissedBy() { return dismissedBy; }
    public void setDismissedBy(String dismissedBy) { this.dismissedBy = dismissedBy; }

    public Boolean getIsBroadcast() { return isBroadcast; }
    public void setIsBroadcast(Boolean isBroadcast) { this.isBroadcast = isBroadcast; }

    public NotificationPriority getPriority() { return priority; }
    public void setPriority(NotificationPriority priority) { this.priority = priority; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
