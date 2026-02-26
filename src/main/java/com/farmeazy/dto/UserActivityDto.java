package com.farmeazy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

public class UserActivityDto {
    private Long id;
    private String activityType;
    private String description;
    private String details;
    private String relatedEntityId;
    private String relatedEntityType;
    private LocalDateTime createdAt;
    private String timeAgo; // e.g., "2 hours ago", "3 days ago"

    public UserActivityDto() {
    }

    public UserActivityDto(Long id, String activityType, String description, String details, String relatedEntityId, String relatedEntityType, LocalDateTime createdAt, String timeAgo) {
        this.id = id;
        this.activityType = activityType;
        this.description = description;
        this.details = details;
        this.relatedEntityId = relatedEntityId;
        this.relatedEntityType = relatedEntityType;
        this.createdAt = createdAt;
        this.timeAgo = timeAgo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getRelatedEntityId() {
        return relatedEntityId;
    }

    public void setRelatedEntityId(String relatedEntityId) {
        this.relatedEntityId = relatedEntityId;
    }

    public String getRelatedEntityType() {
        return relatedEntityType;
    }

    public void setRelatedEntityType(String relatedEntityType) {
        this.relatedEntityType = relatedEntityType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getTimeAgo() {
        return timeAgo;
    }

    public void setTimeAgo(String timeAgo) {
        this.timeAgo = timeAgo;
    }

    public String getIcon() {
        if (activityType == null) return "📝";
        
        return switch (activityType) {
            case "REGISTERED" -> "✍️";
            case "LOGGED_IN" -> "🔓";
            case "LOGGED_OUT" -> "🔒";
            case "ADDED_PRODUCT" -> "➕";
            case "REMOVED_PRODUCT" -> "➖";
            case "UPDATED_PRODUCT" -> "✏️";
            case "ADDED_TO_CART" -> "🛒";
            case "REMOVED_FROM_CART" -> "🗑️";
            case "ORDER_PLACED" -> "📦";
            case "ORDER_PAID" -> "💳";
            case "ORDER_SHIPPED" -> "🚚";
            case "ORDER_DELIVERED" -> "✅";
            case "ORDER_CANCELLED" -> "❌";
            case "COINS_EARNED" -> "🪙";
            case "COINS_USED" -> "💰";
            case "PASSWORD_CHANGED" -> "🔐";
            case "PROFILE_UPDATED" -> "👤";
            case "FARM_ADDED" -> "🌾";
            case "FARM_UPDATED" -> "🔄";
            case "FARM_DELETED" -> "❌";
            case "CROP_ADDED" -> "🌱";
            case "CROP_UPDATED" -> "🔄";
            case "CROP_DELETED" -> "❌";
            case "IRRIGATION_ADDED" -> "💧";
            case "IRRIGATION_UPDATED" -> "🔄";
            case "IRRIGATION_DELETED" -> "❌";
            case "PAYMENT_FAILED" -> "⚠️";
            default -> "📝";
        };
    }
}
