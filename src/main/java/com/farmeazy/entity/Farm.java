package com.farmeazy.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalDateTime;

/**
 * FARM ENTITY CLASS
 * 
 * PURPOSE: Represents a physical farm managed by a user in the FarmEazy system.
 * Contains farm details like name, location, size, soil type, and water source.
 * 
 * KEY COMPONENTS:
 * 1. Farm Information: Name, location, area size, soil type, water source, description
 * 2. Owner: References to the User who owns/manages this farm
 * 3. Collections: Crops planted on this farm and Irrigation schedules for the farm
 * 4. Timestamps: createdAt (immutable) and updatedAt (auto-updated)
 * 
 * HOW IT WORKS:
 * - Each farm belongs to exactly one user (Many-to-One relationship)
 * - One farm can have multiple crops (One-to-Many relationship)
 * - One farm can have multiple irrigation schedules (One-to-Many relationship)
 * - When a farm is deleted, all associated crops and irrigation schedules are also deleted
 * - Timestamps automatically track farm creation and modification dates
 * 
 * DATABASE TABLE: "farms"
 * - Stores farm information and manages farm hierarchy
 * - Foreign key: user_id references the owner of the farm
 */
@Entity
@Table(name = "farms")
public class Farm {
    
    /**
     * UNIQUE IDENTIFIER FOR FARM
     * - Auto-generated primary key
     * - Used to uniquely identify each farm in the system
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * FARM NAME - DISPLAY NAME
     * - Cannot be null
     * - User-friendly name for the farm (e.g., "North Field Farm", "Greenville Fields")
     * - Used in UI and farm listings
     * - Can be searched by users
     */
    @Column(nullable = false)
    private String farmName;
    
    /**
     * FARM LOCATION - GEOGRAPHIC LOCATION
     * - Cannot be null
     * - Describes the physical location/address of the farm
     * - Used to identify farm's geographic coordinates or region
     */
    @Column(nullable = false)
    private String location;
    
    /**
     * FARM AREA SIZE - FARM ACREAGE/HECTARES
     * - Cannot be null
     * - Measured in hectares (1 hectare = ~2.47 acres)
     * - Used for calculating crop density and yield estimates
     * - Important for irrigation planning and resource allocation
     */
    @Column(nullable = false)
    private Double areaSize; // in hectares
    
    /**
     * SOIL TYPE - FARM SOIL CLASSIFICATION
     * - Optional field
     * - Examples: "Loamy", "Sandy", "Clay", "Silty Loam"
     * - Helps determine crop suitability and irrigation needs
     * - Used for agricultural recommendations
     */
    @Column
    private String soilType;
    
    /**
     * WATER SOURCE - PRIMARY IRRIGATION SOURCE
     * - Optional field
     * - Examples: "Borewell", "River", "Canal", "Pond", "Rainwater"
     * - Critical for irrigation scheduling
     * - Used to determine water availability
     */
    @Column
    private String waterSource;
    
    /**
     * FARM DESCRIPTION - ADDITIONAL DETAILS
     * - Optional field
     * - Free-form text for additional farm information
     * - Used to store notes about farm conditions, setup, or special requirements
     */
    @Column
    private String description;
    
    /**
     * RELATIONSHIP: MANY FARMS TO ONE USER (OWNER)
     * - fetch = FetchType.LAZY: User loaded only when explicitly accessed
     * - nullable = false: Every farm must have an owner
     * - JoinColumn: "user_id" column in farms table stores the foreign key
     * - Used to enforce user isolation and security (users can only access their own farms)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * RELATIONSHIP: ONE FARM TO MANY CROPS
     * - mappedBy = "farm": Crop entity has 'farm' field that owns this relationship
     * - cascade = CascadeType.ALL: When farm is deleted, all crops are also deleted
     * - fetch = FetchType.LAZY: Crops loaded only when explicitly accessed (performance)
     * - Used to manage all crops planted on this farm
     */
    @OneToMany(mappedBy = "farm", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Crop> crops = new HashSet<>();
    
    /**
     * RELATIONSHIP: ONE FARM TO MANY IRRIGATION SCHEDULES
     * - mappedBy = "farm": IrrigationSchedule entity has 'farm' field
     * - cascade = CascadeType.ALL: When farm is deleted, all schedules are deleted
     * - fetch = FetchType.LAZY: Schedules loaded only when explicitly accessed
     * - Used to manage all irrigation activities for this farm
     */
    @OneToMany(mappedBy = "farm", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<IrrigationSchedule> irrigationSchedules = new HashSet<>();
    
    /**
     * FARM CREATION TIMESTAMP
     * - Cannot be null, not updatable (immutable)
     * - Automatically set when farm is first created
     * - Used to track farm registration date
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * LAST MODIFICATION TIMESTAMP
     * - Cannot be null
     * - Automatically updated whenever farm data is modified
     * - Used to track when farm information was last changed
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    public Farm() {}
    
    public Farm(Long id, String farmName, String location, Double areaSize, String soilType, String waterSource, String description, User user) {
        this.id = id;
        this.farmName = farmName;
        this.location = location;
        this.areaSize = areaSize;
        this.soilType = soilType;
        this.waterSource = waterSource;
        this.description = description;
        this.user = user;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFarmName() { return farmName; }
    public void setFarmName(String farmName) { this.farmName = farmName; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Double getAreaSize() { return areaSize; }
    public void setAreaSize(Double areaSize) { this.areaSize = areaSize; }
    public String getSoilType() { return soilType; }
    public void setSoilType(String soilType) { this.soilType = soilType; }
    public String getWaterSource() { return waterSource; }
    public void setWaterSource(String waterSource) { this.waterSource = waterSource; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Set<Crop> getCrops() { return crops; }
    public void setCrops(Set<Crop> crops) { this.crops = crops; }
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
