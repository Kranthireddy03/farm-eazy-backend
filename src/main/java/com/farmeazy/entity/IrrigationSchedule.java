package com.farmeazy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * IRRIGATION SCHEDULE ENTITY CLASS
 * 
 * PURPOSE: Represents a scheduled irrigation event for a specific crop on a farm.
 * Tracks irrigation timing, water amounts, completion status, and actual water usage.
 * 
 * KEY COMPONENTS:
 * 1. Scheduling: Irrigation date, start time, duration
 * 2. Water Details: Planned water amount, actual water used
 * 3. Status: Current state (SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED)
 * 4. References: Which crop and farm this irrigation is for
 * 5. Execution: Completion time and notes
 * 6. Timestamps: createdAt and updatedAt for tracking
 * 
 * HOW IT WORKS:
 * - Each irrigation event is linked to a specific crop and farm
 * - Farmers create schedules in advance (SCHEDULED state)
 * - Status transitions: SCHEDULED → IN_PROGRESS → COMPLETED
 * - Actual water usage can differ from planned amount
 * - Timestamps are automatically managed by @PrePersist and @PreUpdate
 * - When crop is deleted, all its irrigation schedules are automatically deleted
 * 
 * DATABASE TABLE: "irrigation_schedules"
 * - Stores irrigation event records for crops
 * - Foreign keys: crop_id and farm_id for relationships
 */
@Entity
@Table(name = "irrigation_schedules")
public class IrrigationSchedule {
    
    /**
     * UNIQUE IDENTIFIER FOR IRRIGATION SCHEDULE
     * - Auto-generated primary key
     * - Used to uniquely identify each irrigation event
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * IRRIGATION DATE - WHEN IRRIGATION IS SCHEDULED
     * - Cannot be null
     * - Date when irrigation is planned to occur
     * - Used to schedule irrigation in advance
     * - Compared with current date to determine upcoming schedules
     */
    @Column(nullable = false)
    private LocalDate irrigationDate;
    
    /**
     * START TIME - WHEN IRRIGATION BEGINS
     * - Cannot be null
     * - Time of day when irrigation should start (e.g., 6:00 AM)
     * - Combined with date for exact scheduling
     * - Used for farmer notifications and timing
     */
    @Column(nullable = false)
    private LocalTime startTime;
    
    /**
     * DURATION - HOW LONG IRRIGATION RUNS
     * - Cannot be null
     * - Duration of irrigation in minutes
     * - Determines how long water is supplied to the crop
     * - Used to calculate end time and plan farm activities
     */
    @Column(nullable = false)
    private Integer duration; // in minutes
    
    /**
     * PLANNED WATER AMOUNT - SCHEDULED WATER QUANTITY
     * - Cannot be null
     * - Amount of water planned for this irrigation (in liters or cubic meters)
     * - Based on crop water requirements and soil conditions
     * - Used for water resource planning
     */
    @Column(nullable = false)
    private Double waterAmount; // in liters or cubic meters
    
    /**
     * IRRIGATION STATUS - CURRENT STATE OF IRRIGATION EVENT
     * - Cannot be null, defaults to "SCHEDULED"
     * - Possible values: SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
     * - SCHEDULED: Waiting to be executed
     * - IN_PROGRESS: Currently running
     * - COMPLETED: Finished execution
     * - CANCELLED: Cancelled before execution
     * - Used for filtering and monitoring irrigation activities
     */
    @Column(nullable = false)
    private String status = "SCHEDULED"; // SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
    
    /**
     * COMPLETION TIMESTAMP - WHEN IRRIGATION WAS COMPLETED
     * - Optional field
     * - Set when irrigation status changes to COMPLETED
     * - Records exact time irrigation finished
     * - Used for irrigation duration tracking
     */
    @Column
    private LocalDateTime completedAt;
    
    /**
     * IRRIGATION NOTES - ADDITIONAL INFORMATION
     * - Optional field
     * - Free-form text for observations during irrigation
     * - Examples: "Weather changed", "Unexpected pump issue", "Soil moisture adequate"
     * - Used for record-keeping and analysis
     */
    @Column
    private String notes;
    
    /**
     * ACTUAL WATER USED - REAL WATER CONSUMPTION
     * - Optional field
     * - Actual amount of water delivered during execution
     * - May differ from planned waterAmount due to:
     *   - Pressure changes, meter inaccuracy, manual adjustments
     * - Used for water usage analysis and optimization
     * - Set after irrigation is completed
     */
    @Column
    private Double actualWaterUsed;
    
    /**
     * RELATIONSHIP: MANY SCHEDULES TO ONE CROP
     * - fetch = FetchType.LAZY: Crop loaded only when explicitly accessed
     * - nullable = false: Every schedule must be linked to a crop
     * - JoinColumn: "crop_id" column stores the foreign key
     * - Used to associate irrigation with a specific crop
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id", nullable = false)
    private Crop crop;
    
    /**
     * RELATIONSHIP: MANY SCHEDULES TO ONE FARM
     * - fetch = FetchType.LAZY: Farm loaded only when explicitly accessed
     * - nullable = false: Every schedule must be linked to a farm
     * - JoinColumn: "farm_id" column stores the foreign key
     * - Used for farm-level irrigation tracking and user isolation
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;
    
    /**
     * IRRIGATION RECORD CREATION TIMESTAMP
     * - Cannot be null, not updatable (immutable)
     * - Automatically set via @PrePersist method
     * - Records when schedule was created
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * LAST MODIFICATION TIMESTAMP
     * - Cannot be null
     * - Automatically updated via @PreUpdate method
     * - Updated whenever schedule status or details change
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    public IrrigationSchedule() {}
    
    public IrrigationSchedule(Long id, LocalDate irrigationDate, LocalTime startTime, Integer duration, Double waterAmount, String status, LocalDateTime completedAt, String notes, Double actualWaterUsed, Crop crop, Farm farm) {
        this.id = id;
        this.irrigationDate = irrigationDate;
        this.startTime = startTime;
        this.duration = duration;
        this.waterAmount = waterAmount;
        this.status = status;
        this.completedAt = completedAt;
        this.notes = notes;
        this.actualWaterUsed = actualWaterUsed;
        this.crop = crop;
        this.farm = farm;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getIrrigationDate() { return irrigationDate; }
    public void setIrrigationDate(LocalDate irrigationDate) { this.irrigationDate = irrigationDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public Double getWaterAmount() { return waterAmount; }
    public void setWaterAmount(Double waterAmount) { this.waterAmount = waterAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Double getActualWaterUsed() { return actualWaterUsed; }
    public void setActualWaterUsed(Double actualWaterUsed) { this.actualWaterUsed = actualWaterUsed; }
    public Crop getCrop() { return crop; }
    public void setCrop(Crop crop) { this.crop = crop; }
    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
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
