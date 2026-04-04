package com.farmeazy.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * IrrigationHistory Entity
 * Tracks actual farmer irrigation actions for audit trail and analytics
 */
@Entity
@Table(name = "irrigation_history")
public class IrrigationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long farmId;

    @Column(nullable = false)
    private Long cropId;

    @Column(nullable = false)
    private LocalDate plannedIrrigationDate;

    @Column(nullable = false)
    private LocalDate actualIrrigationDate;

    @Column
    private Double plannedWaterQuantityMm;

    @Column
    private Double actualWaterUsedMm;

    @Column
    private String status; // COMPLETED, SKIPPED, OVERDUE, RESCHEDULED

    @Column
    private String reasonForSkip;

    @Column
    private String farmerNotes;

    @Column
    private Double waterEfficiencyPercentage; // (actual/planned)*100

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public IrrigationHistory() {}

    public IrrigationHistory(Long userId, Long farmId, Long cropId, LocalDate plannedIrrigationDate,
                            LocalDate actualIrrigationDate, Double plannedWaterQuantityMm,
                            Double actualWaterUsedMm, String status) {
        this.userId = userId;
        this.farmId = farmId;
        this.cropId = cropId;
        this.plannedIrrigationDate = plannedIrrigationDate;
        this.actualIrrigationDate = actualIrrigationDate;
        this.plannedWaterQuantityMm = plannedWaterQuantityMm;
        this.actualWaterUsedMm = actualWaterUsedMm;
        this.status = status;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }

    public Long getCropId() { return cropId; }
    public void setCropId(Long cropId) { this.cropId = cropId; }

    public LocalDate getPlannedIrrigationDate() { return plannedIrrigationDate; }
    public void setPlannedIrrigationDate(LocalDate plannedIrrigationDate) { this.plannedIrrigationDate = plannedIrrigationDate; }

    public LocalDate getActualIrrigationDate() { return actualIrrigationDate; }
    public void setActualIrrigationDate(LocalDate actualIrrigationDate) { this.actualIrrigationDate = actualIrrigationDate; }

    public Double getPlannedWaterQuantityMm() { return plannedWaterQuantityMm; }
    public void setPlannedWaterQuantityMm(Double plannedWaterQuantityMm) { this.plannedWaterQuantityMm = plannedWaterQuantityMm; }

    public Double getActualWaterUsedMm() { return actualWaterUsedMm; }
    public void setActualWaterUsedMm(Double actualWaterUsedMm) { this.actualWaterUsedMm = actualWaterUsedMm; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReasonForSkip() { return reasonForSkip; }
    public void setReasonForSkip(String reasonForSkip) { this.reasonForSkip = reasonForSkip; }

    public String getFarmerNotes() { return farmerNotes; }
    public void setFarmerNotes(String farmerNotes) { this.farmerNotes = farmerNotes; }

    public Double getWaterEfficiencyPercentage() { return waterEfficiencyPercentage; }
    public void setWaterEfficiencyPercentage(Double waterEfficiencyPercentage) { this.waterEfficiencyPercentage = waterEfficiencyPercentage; }

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
