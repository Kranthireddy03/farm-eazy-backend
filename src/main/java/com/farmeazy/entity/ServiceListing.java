package com.farmeazy.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

/**
 * Enhanced ServiceListing entity for marketplace functionality.
 * Supports different service types with dynamic pricing (machine + driver).
 */
@Entity
@Table(name = "service_listings")
public class ServiceListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Display Service ID - Human readable format (SV00001, SV00002, etc.)
     * Auto-generated on service creation
     */
    @Column(name = "display_id", unique = true, length = 10)
    private String displayId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType type;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private Double rate;

    // Enhanced pricing fields
    @Column(name = "base_price", precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "has_driver")
    private Boolean hasDriver = false;

    @Column(name = "driver_price", precision = 10, scale = 2)
    private BigDecimal driverPrice;

    @Column(name = "machine_price", precision = 10, scale = 2)
    private BigDecimal machinePrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_unit", length = 50)
    private PriceUnit priceUnit = PriceUnit.PER_HOUR;

    // Equipment details
    @Column(name = "fuel_included")
    private Boolean fuelIncluded = false;

    @Column(name = "operator_included")
    private Boolean operatorIncluded = false;

    @Column(name = "minimum_hours")
    private Integer minimumHours = 1;

        // Vendor Transparency Fields
        @Column(name = "vendor_id")
        private Long vendorId;
        @Column(name = "vendor_name")
        private String vendorName;
        @Column(name = "vendor_location")
        private String vendorLocation;
        @Column(name = "vendor_type")
        private String vendorType;

    @Column(name = "maximum_hours")
    private Integer maximumHours;

    @Column(name = "service_radius_km")
    private Integer serviceRadiusKm = 50;

    @Column(name = "equipment_power", length = 50)
    private String equipmentPower; // HP for tractors

    @Column(name = "equipment_model", length = 100)
    private String equipmentModel;

    @Column(name = "implements_available", columnDefinition = "TEXT")
    private String implementsAvailable; // JSON array of available implements

    // Manual labor specific
    @Column(name = "workers_count")
    private Integer workersCount;

    @Column(name = "tools_included")
    private Boolean toolsIncluded = false;

    @Column(name = "experience_years")
    private Integer experienceYears;

    // Status and payout
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_status", length = 50)
    private PayoutStatus payoutStatus = PayoutStatus.NOT_APPLICABLE;

    @Column(name = "contact_name", nullable = false)
    private String contactName;

    @Column(name = "contact_phone", nullable = false)
    private String contactPhone;

    private String contactEmail;

    @Column(nullable = false)
    private String availability;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "serviceListing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceAttribute> attributes = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ServiceType {
        TRACTOR,
        JCB,
        MANUAL,
        IRRIGATION,
        HARVESTER,
        SPRAYER,
        TRANSPORT
    }

    public enum PriceUnit {
        PER_HOUR,
        PER_DAY,
        PER_ACRE,
        PER_HECTARE,
        FIXED
    }

    public enum PayoutStatus {
        NOT_APPLICABLE,
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    public ServiceListing() {}

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Calculate total price based on machine + driver (if applicable)
     */
    public BigDecimal calculateTotalPrice() {
        BigDecimal total = machinePrice != null ? machinePrice : BigDecimal.ZERO;
        if (hasDriver != null && hasDriver && driverPrice != null) {
            total = total.add(driverPrice);
        }
        return total;
    }
    

    // Duplicate fields removed. Only one set of fields and constructors remain.

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDisplayId() {
        return displayId;
    }

    public void setDisplayId(String displayId) {
        this.displayId = displayId;
    }

    /**
     * Generate display ID in format SV00001
     */
    public void generateDisplayId() {
        if (this.id != null && this.displayId == null) {
            this.displayId = String.format("SV%05d", this.id);
        }
    }

    public ServiceType getType() {
        return type;
    }

    public void setType(ServiceType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // New getters and setters for enhanced fields
    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Boolean getHasDriver() {
        return hasDriver;
    }

    public void setHasDriver(Boolean hasDriver) {
        this.hasDriver = hasDriver;
    }

    public BigDecimal getDriverPrice() {
        return driverPrice;
    }

    public void setDriverPrice(BigDecimal driverPrice) {
        this.driverPrice = driverPrice;
    }

    public BigDecimal getMachinePrice() {
        return machinePrice;
    }

    public void setMachinePrice(BigDecimal machinePrice) {
        this.machinePrice = machinePrice;
    }

    public PriceUnit getPriceUnit() {
        return priceUnit;
    }

    public void setPriceUnit(PriceUnit priceUnit) {
        this.priceUnit = priceUnit;
    }

    public Boolean getFuelIncluded() {
        return fuelIncluded;
    }

    public void setFuelIncluded(Boolean fuelIncluded) {
        this.fuelIncluded = fuelIncluded;
    }

    public Boolean getOperatorIncluded() {
        return operatorIncluded;
    }

    public void setOperatorIncluded(Boolean operatorIncluded) {
        this.operatorIncluded = operatorIncluded;
    }

    public Integer getMinimumHours() {
        return minimumHours;
    }

    public void setMinimumHours(Integer minimumHours) {
        this.minimumHours = minimumHours;
    }

    public Integer getMaximumHours() {
        return maximumHours;
    }

    public void setMaximumHours(Integer maximumHours) {
        this.maximumHours = maximumHours;
    }

    public Integer getServiceRadiusKm() {
        return serviceRadiusKm;
    }

    public void setServiceRadiusKm(Integer serviceRadiusKm) {
        this.serviceRadiusKm = serviceRadiusKm;
    }

    public String getEquipmentPower() {
        return equipmentPower;
    }

    public void setEquipmentPower(String equipmentPower) {
        this.equipmentPower = equipmentPower;
    }

    public String getEquipmentModel() {
        return equipmentModel;
    }

    public void setEquipmentModel(String equipmentModel) {
        this.equipmentModel = equipmentModel;
    }

    public String getImplementsAvailable() {
        return implementsAvailable;
    }

    public void setImplementsAvailable(String implementsAvailable) {
        this.implementsAvailable = implementsAvailable;
    }

    public Integer getWorkersCount() {
        return workersCount;
    }

    public void setWorkersCount(Integer workersCount) {
        this.workersCount = workersCount;
    }

    public Boolean getToolsIncluded() {
        return toolsIncluded;
    }

    public void setToolsIncluded(Boolean toolsIncluded) {
        this.toolsIncluded = toolsIncluded;
    }

    public Integer getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public PayoutStatus getPayoutStatus() {
        return payoutStatus;
    }

    public void setPayoutStatus(PayoutStatus payoutStatus) {
        this.payoutStatus = payoutStatus;
    }

    public List<ServiceAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<ServiceAttribute> attributes) {
        this.attributes = attributes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
