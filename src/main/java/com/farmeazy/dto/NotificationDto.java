package com.farmeazy.dto;

import com.farmeazy.entity.Notification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * NOTIFICATION DTO
 * 
 * Transfer object for notification data between API and frontend.
 */
public class NotificationDto {

    private Long id;
    private String type;
    private String title;
    private String message;
    private String actionUrl;
    private Boolean isRead;
    private Boolean isBroadcast;
    private String priority;
    private LocalDateTime createdAt;
    private String timeAgo; // "2 hours ago", "Just now"

    // Default constructor
    public NotificationDto() {}

    // Convert from Entity
    public static NotificationDto fromEntity(Notification n) {
        NotificationDto dto = new NotificationDto();
        dto.setId(n.getId());
        dto.setType(n.getType().name());
        dto.setTitle(n.getTitle());
        dto.setMessage(n.getMessage());
        dto.setActionUrl(n.getActionUrl());
        dto.setIsRead(n.getIsRead());
        dto.setIsBroadcast(n.getIsBroadcast());
        dto.setPriority(n.getPriority().name());
        dto.setCreatedAt(n.getCreatedAt());
        dto.setTimeAgo(calculateTimeAgo(n.getCreatedAt()));
        return dto;
    }

    // Calculate human-readable time ago
    private static String calculateTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(dateTime, now).toMinutes();
        
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " min ago";
        
        long hours = minutes / 60;
        if (hours < 24) return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        
        long days = hours / 24;
        if (days < 7) return days + " day" + (days > 1 ? "s" : "") + " ago";
        
        long weeks = days / 7;
        if (weeks < 4) return weeks + " week" + (weeks > 1 ? "s" : "") + " ago";
        
        return dateTime.toLocalDate().toString();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public Boolean getIsBroadcast() { return isBroadcast; }
    public void setIsBroadcast(Boolean isBroadcast) { this.isBroadcast = isBroadcast; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getTimeAgo() { return timeAgo; }
    public void setTimeAgo(String timeAgo) { this.timeAgo = timeAgo; }

    /**
     * Request DTO for creating notifications (Admin use)
     */
    public static class CreateNotificationRequest {
        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title must be under 100 characters")
        private String title;

        @NotBlank(message = "Message is required")
        @Size(max = 500, message = "Message must be under 500 characters")
        private String message;

        private String type = "SYSTEM";
        private String priority = "NORMAL";
        private String actionUrl;
        private Boolean isBroadcast = false;
        private Long userId; // For user-specific notification
        private Integer expiresInDays = 30;

        // Getters and Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public String getActionUrl() { return actionUrl; }
        public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }

        public Boolean getIsBroadcast() { return isBroadcast; }
        public void setIsBroadcast(Boolean isBroadcast) { this.isBroadcast = isBroadcast; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Integer getExpiresInDays() { return expiresInDays; }
        public void setExpiresInDays(Integer expiresInDays) { this.expiresInDays = expiresInDays; }
    }

    /**
     * Response for notification count (unread badge)
     */
    public static class NotificationCountResponse {
        private Long unreadCount;
        private Long totalCount;

        public NotificationCountResponse(Long unreadCount, Long totalCount) {
            this.unreadCount = unreadCount;
            this.totalCount = totalCount;
        }

        public Long getUnreadCount() { return unreadCount; }
        public void setUnreadCount(Long unreadCount) { this.unreadCount = unreadCount; }

        public Long getTotalCount() { return totalCount; }
        public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
    }
}
