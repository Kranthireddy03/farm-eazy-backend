package com.farmeazy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CropIrrigationRule - Rule-based engine for irrigation recommendations
 * 
 * Stores rules for different crops based on:
 * - Crop type (rice, wheat, sugarcane, etc.)
 * - Soil type (clay, loam, sandy, etc.)
 * - Season (kharif, rabi, summer)
 * - Region (Karnataka, Punjab, etc.)
 * 
 * Logic:
 * - Interval: Days between irrigations
 * - Water quantity: mm or liters per hectare
 * - Temperature factor: Adjust for heat
 * 
 * Example:
 * Crop: Rice | Soil: Clay | Season: Kharif | Region: Karnataka
 * => Interval: 3 days | Water: 50mm | Temp Factor: 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "crop_irrigation_rules", indexes = {
        @Index(name = "idx_crop_soil_season", columnList = "crop_type,soil_type,season,region"),
        @Index(name = "idx_crop_region", columnList = "crop_type,region")
})
public class CropIrrigationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cropType; // e.g., "Rice", "Wheat", "Sugarcane"

    @Column(nullable = false)
    private String soilType; // e.g., "Clay", "Loam", "Sandy"

    @Column(nullable = false)
    private String season; // e.g., "KHARIF", "RABI", "SUMMER"

    @Column(nullable = false)
    private String region; // e.g., "KARNATAKA", "PUNJAB", "MAHARASHTRA"

    @Column(nullable = false)
    private Integer irrigationIntervalDays; // Days between irrigations (e.g., 3, 5, 7)

    @Column(nullable = false)
    private Double waterQuantityMm; // Water required in mm per irrigation

    @Column(precision = 3, scale = 2)
    private BigDecimal temperatureFactor = BigDecimal.ONE; // Adjustment for heat (1.0 = normal)

    @Column(columnDefinition = "TEXT")
    private String description; // e.g., "Rice in clay soil during monsoon"

    @Column(columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Explicit accessors used by SmartIrrigationService (kept even with Lombok for build stability)
    public Integer getIrrigationIntervalDays() {
        return irrigationIntervalDays;
    }

    public Double getWaterQuantityMm() {
        return waterQuantityMm;
    }
}
