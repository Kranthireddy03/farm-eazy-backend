package com.farmeazy.dto;

import java.time.LocalDate;

/**
 * DTO for irrigation history (audit trail of farmer actions)
 */
public class IrrigationHistoryDto {
    private Long id;
    private Long cropId;
    private String cropName;
    private LocalDate plannedIrrigationDate;
    private LocalDate actualIrrigationDate;
    private Double plannedWaterQuantityMm;
    private Double actualWaterUsedMm;
    private Double waterEfficiencyPercentage;
    private String status;
    private String farmerNotes;

    // Constructors
    public IrrigationHistoryDto() {}

    public IrrigationHistoryDto(Long id, Long cropId, LocalDate plannedIrrigationDate,
                                LocalDate actualIrrigationDate, Double plannedWaterQuantityMm,
                                Double actualWaterUsedMm, Double waterEfficiencyPercentage,
                                String status) {
        this.id = id;
        this.cropId = cropId;
        this.plannedIrrigationDate = plannedIrrigationDate;
        this.actualIrrigationDate = actualIrrigationDate;
        this.plannedWaterQuantityMm = plannedWaterQuantityMm;
        this.actualWaterUsedMm = actualWaterUsedMm;
        this.waterEfficiencyPercentage = waterEfficiencyPercentage;
        this.status = status;
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCropId() {
        return cropId;
    }

    public void setCropId(Long cropId) {
        this.cropId = cropId;
    }

    public String getCropName() {
        return cropName;
    }

    public void setCropName(String cropName) {
        this.cropName = cropName;
    }

    public LocalDate getPlannedIrrigationDate() {
        return plannedIrrigationDate;
    }

    public void setPlannedIrrigationDate(LocalDate plannedIrrigationDate) {
        this.plannedIrrigationDate = plannedIrrigationDate;
    }

    public LocalDate getActualIrrigationDate() {
        return actualIrrigationDate;
    }

    public void setActualIrrigationDate(LocalDate actualIrrigationDate) {
        this.actualIrrigationDate = actualIrrigationDate;
    }

    public Double getPlannedWaterQuantityMm() {
        return plannedWaterQuantityMm;
    }

    public void setPlannedWaterQuantityMm(Double plannedWaterQuantityMm) {
        this.plannedWaterQuantityMm = plannedWaterQuantityMm;
    }

    public Double getActualWaterUsedMm() {
        return actualWaterUsedMm;
    }

    public void setActualWaterUsedMm(Double actualWaterUsedMm) {
        this.actualWaterUsedMm = actualWaterUsedMm;
    }

    public Double getWaterEfficiencyPercentage() {
        return waterEfficiencyPercentage;
    }

    public void setWaterEfficiencyPercentage(Double waterEfficiencyPercentage) {
        this.waterEfficiencyPercentage = waterEfficiencyPercentage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFarmerNotes() {
        return farmerNotes;
    }

    public void setFarmerNotes(String farmerNotes) {
        this.farmerNotes = farmerNotes;
    }
}
