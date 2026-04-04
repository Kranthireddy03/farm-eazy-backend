package com.farmeazy.dto;

import java.time.LocalDate;

/**
 * DTO for irrigation recommendations
 */
public class IrrigationRecommendationDto {
    private Long cropId;
    private String cropName;
    private LocalDate nextIrrigationDate;
    private Double waterQuantityMm;
    private Integer intervalDays;
    private LocalDate lastIrrigationDate;
    private String urgencyLevel;

    // Constructors
    public IrrigationRecommendationDto() {}

    public IrrigationRecommendationDto(Long cropId, String cropName, LocalDate nextIrrigationDate,
                                        Double waterQuantityMm, Integer intervalDays,
                                        LocalDate lastIrrigationDate, String urgencyLevel) {
        this.cropId = cropId;
        this.cropName = cropName;
        this.nextIrrigationDate = nextIrrigationDate;
        this.waterQuantityMm = waterQuantityMm;
        this.intervalDays = intervalDays;
        this.lastIrrigationDate = lastIrrigationDate;
        this.urgencyLevel = urgencyLevel;
    }

    // Getters & Setters
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

    public LocalDate getNextIrrigationDate() {
        return nextIrrigationDate;
    }

    public void setNextIrrigationDate(LocalDate nextIrrigationDate) {
        this.nextIrrigationDate = nextIrrigationDate;
    }

    public Double getWaterQuantityMm() {
        return waterQuantityMm;
    }

    public void setWaterQuantityMm(Double waterQuantityMm) {
        this.waterQuantityMm = waterQuantityMm;
    }

    public Integer getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(Integer intervalDays) {
        this.intervalDays = intervalDays;
    }

    public LocalDate getLastIrrigationDate() {
        return lastIrrigationDate;
    }

    public void setLastIrrigationDate(LocalDate lastIrrigationDate) {
        this.lastIrrigationDate = lastIrrigationDate;
    }

    public String getUrgencyLevel() {
        return urgencyLevel;
    }

    public void setUrgencyLevel(String urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }
}
