package com.farmeazy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_activities", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class UserActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private ActivityType activityType;

    private String description;
    private String details; // JSON string for storing additional info
    private String relatedEntityId; // ID of related entity (product, order, farm, etc)
    private String relatedEntityType; // Type of related entity

    private LocalDateTime createdAt;

    public UserActivity() {
    }

    public UserActivity(User user, ActivityType activityType, String description, String details, String relatedEntityId, String relatedEntityType) {
        this.user = user;
        this.activityType = activityType;
        this.description = description;
        this.details = details;
        this.relatedEntityId = relatedEntityId;
        this.relatedEntityType = relatedEntityType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(ActivityType activityType) {
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

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public enum ActivityType {
        REGISTERED,
        LOGGED_IN,
        LOGGED_OUT,
        
        FARM_CREATED,
        FARM_UPDATED,
        FARM_DELETED,
        
        CROP_PLANTED,
        CROP_UPDATED,
        CROP_DELETED,
        
        IRRIGATION_SCHEDULED,
        IRRIGATION_UPDATED,
        IRRIGATION_DELETED,
        IRRIGATION_COMPLETED,
        
        PRODUCT_LISTED,
        PRODUCT_UPDATED,
        PRODUCT_DELETED,
        PRODUCT_STATUS_CHANGED,
        PASSWORD_CHANGED,
        
        ORDER_PLACED,
        ORDER_CANCELLED,
        ORDER_DELIVERED,
        ORDER_PAID,
        COINS_USED,
        PAYMENT_FAILED,
        
        PAYMENT_SUCCESS,
        SERVICE_BOOKED,
        SERVICE_CANCELLED,
        
        REFUND_INITIATED
    }
}
