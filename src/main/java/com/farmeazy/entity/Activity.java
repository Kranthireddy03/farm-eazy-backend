package com.farmeazy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * ACTIVITY ENTITY - USER ACTIVITY LOG
 * 
 * PURPOSE: Tracks all user actions (farm creation, crop planting, irrigation scheduling, etc.)
 * Each activity is associated with a specific user and includes timestamp and description
 * 
 * FIELDS:
 * - id: Unique activity identifier (auto-generated)
 * - user: User who performed the activity (foreign key to users table)
 * - activityType: Type of activity (CREATE_FARM, CREATE_CROP, CREATE_IRRIGATION, DELETE_FARM, etc.)
 * - description: Human-readable description of the activity
 * - entityType: Type of entity affected (Farm, Crop, IrrigationSchedule)
 * - entityId: ID of the entity that was affected
 * - createdAt: Timestamp when activity occurred (auto-generated)
 * 
 * EXAMPLE:
 * Activity: User "rajesh@example.com" created farm "North Field" on 2026-01-25 14:30:00
 */
@Entity
@Table(name = "activities")
@NoArgsConstructor
@AllArgsConstructor
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String activityType; // CREATE_FARM, CREATE_CROP, CREATE_IRRIGATION, DELETE_FARM, etc.

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description; // e.g., "Created farm 'North Field'"

    @Column(nullable = false, length = 50)
    private String entityType; // Farm, Crop, IrrigationSchedule

    @Column
    private Long entityId; // ID of the affected entity

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getActivityType() {
        return activityType;
    }

    public String getDescription() {
        return description;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
