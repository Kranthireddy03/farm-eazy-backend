package com.farmeazy.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * CROP ENTITY CLASS
 * 
 * PURPOSE: Represents a crop/cultivation planted on a farm.
 * Tracks crop details like name, season, sowing date, expected harvest, status, and yield information.
 * 
 * KEY COMPONENTS:
 * 1. Crop Information: Name, season, variety, planting area
 * 2. Timeline: Sowing date, expected harvest date
 * 3. Production Data: Expected yield, actual yield tracking
 * 4. Status: Current state (PLANTED, GROWING, HARVESTING, HARVESTED)
 * 5. Farm Reference: Which farm this crop belongs to
 * 6. Irrigation: Related irrigation schedules for this crop
 * 7. Timestamps: createdAt and updatedAt for audit trail
 * 
 * HOW IT WORKS:
 * - Each crop belongs to exactly one farm (Many-to-One relationship)
 * - One crop can have multiple irrigation schedules (One-to-Many relationship)
 * - Status tracks crop lifecycle through growing season
 * - Timestamps provide complete audit history
 * - When a crop is deleted, all related irrigation schedules are deleted
 * 
 * DATABASE TABLE: "crops"
 * - Stores information about crops planted on farms
 * - Foreign key: farm_id references the parent farm
 */
@Entity
@Table(name = "crops")
public class Crop {
    
    /**
     * UNIQUE IDENTIFIER FOR CROP
     * - Auto-generated primary key
     * - Used to uniquely identify each crop in the system
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * CROP NAME - CROP TYPE
     * - Cannot be null
     * - Name of the crop being grown (e.g., "Wheat", "Rice", "Tomato", "Cotton")
     * - Used to identify crop type for recommendations and tracking
     */
    @Column(nullable = false)
    private String cropName;
    
    /**
     * CROP SEASON - PLANTING SEASON
     * - Cannot be null
     * - Season when crop is planted (e.g., "Kharif", "Rabi", "Summer")
     * - Important for planning and crop rotation
     */
    @Column(nullable = false)
    private String season;
    
    /**
     * SOWING DATE - WHEN CROP WAS PLANTED
     * - Cannot be null
     * - Date when farmer planted the seeds
     * - Used to calculate crop age and expected maturity
     * - Important for irrigation and fertilizer scheduling
     */
    @Column(nullable = false)
    private LocalDate sowingDate;
    
    /**
     * EXPECTED HARVEST DATE - ESTIMATED MATURITY DATE
     * - Cannot be null
     * - Projected date when crop will be ready for harvest
     * - Used to plan harvesting and post-harvest activities
     * - Based on crop variety and growing conditions
     */
    @Column(nullable = false)
    private LocalDate expectedHarvestDate;
    
    /**
     * CROP VARIETY - SPECIFIC CULTIVAR/BREED
     * - Optional field
     * - Specific variety within crop type (e.g., "Basmati", "IR64" for rice)
     * - Different varieties have different water/nutrient needs
     * - Used for agricultural recommendations
     */
    @Column
    private String variety;
    
    /**
     * PLANTING AREA - SIZE OF CROP FIELD
     * - Optional field
     * - Area where crop is planted in hectares
     * - Must be less than or equal to total farm area
     * - Used to calculate total yield and resource requirements
     */
    @Column
    private Double plantingArea; // in hectares
    
    /**
     * EXPECTED YIELD - PROJECTED PRODUCTION
     * - Optional field
     * - Expected quantity of crop to be harvested in kilograms (kg)
     * - Used for yield prediction and farm planning
     * - Compared with actual yield after harvest
     */
    @Column
    private Integer expectedYield; // in kg
    
    /**
     * CROP NOTES - ADDITIONAL INFORMATION
     * - Optional field
     * - Free-form text for specific details about this crop
     * - Examples: disease notes, special care requirements, observations
     */
    @Column
    private String notes;
    
    /**
     * CROP STATUS - CURRENT LIFECYCLE STATE
     * - Cannot be null, defaults to "PLANTED"
     * - Possible values: PLANTED, GROWING, HARVESTING, HARVESTED
     * - Tracks where crop is in its lifecycle
     * - Used for filtering and crop management workflows
     */
    @Column(nullable = false)
    private String status = "PLANTED"; // PLANTED, GROWING, HARVESTING, HARVESTED
    
    /**
     * RELATIONSHIP: MANY CROPS TO ONE FARM
     * - fetch = FetchType.LAZY: Farm loaded only when explicitly accessed
     * - nullable = false: Every crop must belong to a farm
     * - JoinColumn: "farm_id" column stores the foreign key
     * - Used to enforce farm isolation (crops belong to specific farms)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;
    
    /**
     * RELATIONSHIP: ONE CROP TO MANY IRRIGATION SCHEDULES
     * - mappedBy = "crop": IrrigationSchedule owns this relationship
     * - cascade = CascadeType.ALL: When crop is deleted, schedules are deleted
     * - fetch = FetchType.LAZY: Schedules loaded only when explicitly accessed
     * - Used to manage all irrigation events scheduled for this specific crop
     */
    @OneToMany(mappedBy = "crop", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<IrrigationSchedule> irrigationSchedules = new HashSet<>();
    
    /**
     * CROP RECORD CREATION TIMESTAMP
     * - Cannot be null, not updatable (immutable)
     * - Automatically set when crop record is first created
     * - Used for audit trail
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * LAST MODIFICATION TIMESTAMP
     * - Cannot be null
     * - Automatically updated whenever crop data is modified
     * - Used to track when crop information was last changed
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    public Crop() {}
    
    public Crop(Long id, String cropName, String season, LocalDate sowingDate, LocalDate expectedHarvestDate, String variety, Double plantingArea, Integer expectedYield, String notes, String status, Farm farm) {
        this.id = id;
        this.cropName = cropName;
        this.season = season;
        this.sowingDate = sowingDate;
        this.expectedHarvestDate = expectedHarvestDate;
        this.variety = variety;
        this.plantingArea = plantingArea;
        this.expectedYield = expectedYield;
        this.notes = notes;
        this.status = status;
        this.farm = farm;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }
    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }
    public LocalDate getSowingDate() { return sowingDate; }
    public void setSowingDate(LocalDate sowingDate) { this.sowingDate = sowingDate; }
    public LocalDate getExpectedHarvestDate() { return expectedHarvestDate; }
    public void setExpectedHarvestDate(LocalDate expectedHarvestDate) { this.expectedHarvestDate = expectedHarvestDate; }
    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }
    public Double getPlantingArea() { return plantingArea; }
    public void setPlantingArea(Double plantingArea) { this.plantingArea = plantingArea; }
    public Integer getExpectedYield() { return expectedYield; }
    public void setExpectedYield(Integer expectedYield) { this.expectedYield = expectedYield; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }
    public Set<IrrigationSchedule> getIrrigationSchedules() { return irrigationSchedules; }
    public void setIrrigationSchedules(Set<IrrigationSchedule> irrigationSchedules) { this.irrigationSchedules = irrigationSchedules; }
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
