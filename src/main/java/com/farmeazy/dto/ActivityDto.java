package com.farmeazy.dto;

import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * ACTIVITY DATA TRANSFER OBJECT
 * 
 * PURPOSE: Transfers activity data from backend to frontend
 * Used for displaying recent activities in dashboard
 */
@NoArgsConstructor
public class ActivityDto {
    
    private Long id;
    
    private String activityType; // CREATE_FARM, CREATE_CROP, CREATE_IRRIGATION, etc.
    
    private String description; // Human-readable description
    
    private String entityType; // Farm, Crop, IrrigationSchedule
    
    private Long entityId;
    
    private LocalDateTime createdAt; // When the activity occurred

    // Constructor with all fields
    public ActivityDto(Long id, String activityType, String description, 
                      String entityType, Long entityId, LocalDateTime createdAt) {
        this.id = id;
        this.activityType = activityType;
        this.description = description;
        this.entityType = entityType;
        this.entityId = entityId;
        this.createdAt = createdAt;
    }

    // Getters and Setters
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

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
